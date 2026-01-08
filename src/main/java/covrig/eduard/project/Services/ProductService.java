package covrig.eduard.project.Services;

import covrig.eduard.project.Models.*;
import covrig.eduard.project.Repositories.BrandRepository;
import covrig.eduard.project.Repositories.CategoryRepository;
import covrig.eduard.project.Repositories.ProductRepository;
import covrig.eduard.project.dtos.product.ProductCreationDTO;
import covrig.eduard.project.dtos.product.ProductResponseDTO;
import covrig.eduard.project.mappers.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional //e necesar cand foloesc fetchtype.lazy pe models
@Slf4j //pt cron job, creeaza obiectul log pentru a da log-uri in consola
//default readonly=false -> se foloseste default cu optiunile astea pentru toate metodele publice, daca nu se mentioneaza altfel cu o
//adnotare noua, cum e la primele 3 metode de tip GET.
public class ProductService {
    private final ProductRepository productRepository;
    //FINAL PE CAMP CAND FACI CU CONSTRUCTOR.
    //FARA FINAL, CAND FACI CU AUTOWIRED DIRECT PE EL
    private final ProductMapper productMapper;

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,ProductMapper productMapper,
                          BrandRepository brandRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productMapper=productMapper;
        this.brandRepository=brandRepository;
        this.categoryRepository=categoryRepository;
    }

    public Double calculateSubtotalForQuantity(Product product, Integer requestedQty) {
        if (product.getStockQuantity() <= 0) return 0.0;

        // pretul redus
        Double discountedPrice = getDiscountedPriceOnly(product);

        // Daca nu exista recuceri sau nu se afla in cazul de apropeire de expirare, avem pretul normal
        if (discountedPrice.equals(product.getPrice()) || product.getNearExpiryQuantity() <= 0) {
            return requestedQty * product.getPrice();
        }
        // Aplicam pretul redus doar pe cantitatea care se afla in pragul de expirare
        int qtyAtDiscount = Math.min(requestedQty, product.getNearExpiryQuantity());
        int qtyAtFullPrice = Math.max(0, requestedQty - qtyAtDiscount);

        Double total = (qtyAtDiscount * discountedPrice) + (qtyAtFullPrice * product.getPrice());

        log.info("Calcul pret mixt pentru {}: {} bucati la pret redus, {} la pret plin.",
                product.getName(), qtyAtDiscount, qtyAtFullPrice);

        return total;
    }
    //Cat ar fi pretul redus pentru o singura unitate din lotul critic
    private Double getDiscountedPriceOnly(Product product) {
        if (product.getExpirationDate() == null) return product.getPrice();
        long days = ChronoUnit.DAYS.between(LocalDate.now(), product.getExpirationDate());

        if (days < 0) return product.getPrice() * 0.25;
        if (days < 1) return product.getPrice() * 0.25; // -75%
        if (days <= 3) return product.getPrice() * 0.50; // -50%
        if (days <= 7) return product.getPrice() * 0.80; // -20%

        return product.getPrice();
    }



    private Double applyDiscount(Double originalPrice, Double value,String type)
    {
        if(type.equalsIgnoreCase("PERCENT"))
        {
            return (1-value/100)*originalPrice;
        }
        else if(type.equalsIgnoreCase("FIXED"))
            return Math.max(originalPrice-value,0); //evita cazul cand pretul final e negativ
        return originalPrice;
    }
    public Discount findActiveDiscount(Product product)
    {
        if(product.getDiscounts()==null || product.getDiscounts().isEmpty()) return null;
        Instant now=Instant.now();
        return product.getDiscounts().stream().filter(d ->
                d.getDiscountStartDate().isBefore(now)&&d.getDiscountEndDate().isAfter(now)).findFirst().orElse(null);
    }

    @Scheduled(cron = "0 0 0 * * *") //ora 0 minutul 0 secunda 0, fiecare zi a lunii, fiecare luna din an, fiecare zi a saptamanii
    public void autoManageLotsAndExpirations() {
        log.info("Rulare algoritm automat de gestionare loturi si expirari...");
        List<Product> products = productRepository.findAll();

        for (Product p : products) {
            if (p.getExpirationDate() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), p.getExpirationDate());

                // 1. MARCARE LOT (daca intra azi in zona de 7 zile)
                if (days <= 7 && days >= 0 && p.getNearExpiryQuantity() == 0) {
                    p.setNearExpiryQuantity(p.getStockQuantity());
                    productRepository.save(p);
                    log.info("LOT CRITIC MARCAT: Produsul {} are {} unitati la pret dinamic.", p.getName(), p.getNearExpiryQuantity());
                }

                // 2. ELIMINARE LOT EXPIRAT (Daca data a trecut)
                // scadem tot ce a ramas din lotul marcat ca nevandut
                if (days < 0 && p.getNearExpiryQuantity() > 0) {
                    int expiredQty = p.getNearExpiryQuantity();
                    p.setStockQuantity(Math.max(0, p.getStockQuantity() - expiredQty));
                    p.setNearExpiryQuantity(0); // Lotul a fost eliminat
                    productRepository.save(p);
                    log.warn("ELIMINARE AUTOMATA: {} unitati expirate eliminate pentru {}.", expiredQty, p.getName());
                }
            }
        }
    }
    private ProductResponseDTO enrichProductDto(Product p) {
        ProductResponseDTO dto = productMapper.toDto(p);
        // Pretul afisat va fi cel mai mic disponibil
        Double currentPrice = getDiscountedPriceOnly(p);
        dto.setCurrentPrice(currentPrice);
        dto.setHasActiveDiscount(currentPrice < p.getPrice() && p.getNearExpiryQuantity() > 0);
        return dto;
    }


    //1. CITIRE
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream().map(this::enrichProductDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::enrichProductDto)
                .orElseThrow(() -> new RuntimeException("Produsul cu ID-ul " + id + " nu a fost gasit."));
    }

    //filtrare produse care necesita discount (expira)
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsExpiringBefore(LocalDate date) {
        return productRepository.findByExpirationDateBefore(date).stream()
                .map(this::enrichProductDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByBrandName(String brandName) {
        return productRepository.findByBrandName(brandName).stream()
                .map(this::enrichProductDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategoryName(String categoryName) {
        return productRepository.findByCategoryName(categoryName).stream()
                .map(this::enrichProductDto)
                .collect(Collectors.toList());
    }

    // SCRIERE

    public ProductResponseDTO createProduct(ProductCreationDTO creationDTO) {
        Product productToSave = productMapper.toEntity(creationDTO);
        //mapeaza campurile simple


        //MAPARE CAMPURI COMPLEXE (FK)
        Brand brand = brandRepository.findById(creationDTO.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand-ul cu ID-ul " + creationDTO.getBrandId() + " nu a fost gasit."));
        Category category = categoryRepository.findById(creationDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoria cu ID-ul " + creationDTO.getCategoryId() + " nu a fost gasita."));

        productToSave.setBrand(brand);
        productToSave.setCategory(category);
        productToSave.setNearExpiryQuantity(0); // Produs nou, nu e marcat ca si cum expira curand

        if (creationDTO.getImageUrls() != null) { //seteaza imaginile
            List<ProductImage> images = new java.util.ArrayList<>();
            for (String url : creationDTO.getImageUrls()) {
                ProductImage img = new ProductImage();
                img.setImageUrl(url);
                img.setProduct(productToSave); // setam si legatura inversa pentru imagine -> product
                images.add(img);
            }
            productToSave.setImages(images);
        }

        if (creationDTO.getAttributes() != null) { //gestionare atribute (valori nutritionale, etc)
            List<covrig.eduard.project.Models.ProductAttribute> attrs = new java.util.ArrayList<>();
            creationDTO.getAttributes().forEach((key, value) -> {
                covrig.eduard.project.Models.ProductAttribute attr = new covrig.eduard.project.Models.ProductAttribute();
                attr.setName(key);
                attr.setValue(value);
                attr.setProduct(productToSave); // setam si legatura inversa pentru atribut -> product
                attrs.add(attr);
            });
            productToSave.setAttributes(attrs);
        }

        return enrichProductDto(productRepository.save(productToSave));
    }

    //UPDATE
    public ProductResponseDTO updateProduct(Long id, ProductCreationDTO updateDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produsul cu ID-ul " + id + " nu a fost gasit pentru actualizare."));

        existingProduct.setName(updateDTO.getName());
        existingProduct.setPrice(updateDTO.getPrice());
        existingProduct.setStockQuantity(updateDTO.getStockQuantity());
        existingProduct.setUnitOfMeasure(updateDTO.getUnitOfMeasure());

        //Daca adminul schimba data, se  reseteaza si nearExpiryQuantity
        if (updateDTO.getExpirationDate() != null && !updateDTO.getExpirationDate().equals(existingProduct.getExpirationDate())) {
            existingProduct.setExpirationDate(updateDTO.getExpirationDate());
            existingProduct.setNearExpiryQuantity(0);
        }

        Brand brand = brandRepository.findById(updateDTO.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand-ul cu ID-ul " + updateDTO.getBrandId() + " nu a fost gasit."));
        Category category = categoryRepository.findById(updateDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoria cu ID-ul " + updateDTO.getCategoryId() + " nu a fost gasita."));

        existingProduct.setBrand(brand);
        existingProduct.setCategory(category);

        return enrichProductDto(productRepository.save(existingProduct));
    }

        //DELETE

    public ProductResponseDTO deleteProduct(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produsul cu ID-ul " + id + " nu a fost gasit pentru stergere."));
        productRepository.delete(p);
        return productMapper.toDto(p);
    }
    }



