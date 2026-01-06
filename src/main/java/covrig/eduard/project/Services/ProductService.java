package covrig.eduard.project.Services;

import covrig.eduard.project.Models.Brand;
import covrig.eduard.project.Models.Category;
import covrig.eduard.project.Models.Discount;
import covrig.eduard.project.Models.Product;
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

    @Scheduled(cron = "0 0 0 * * *")
    public void autoManageLotsAndExpirations() {
        log.info("Rulare algoritm automat de marcare loturi critice...");
        List<Product> products = productRepository.findAll();

        for (Product p : products) {
            if (p.getExpirationDate() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), p.getExpirationDate());

                // Daca produsul intra azi in perioada de 7 zile si nu era marcat inainte
                if (days <= 7 && days >= 0 && p.getNearExpiryQuantity() == 0) {
                    p.setNearExpiryQuantity(p.getStockQuantity());
                    productRepository.save(p);
                    log.warn("LOT CRITIC ACTIVAT: Produsul {} are acum {} unitati la reducere dinamica.",
                            p.getName(), p.getNearExpiryQuantity());
                }

                // Stoc -1 daca a expirat
                if (days < 0 && p.getStockQuantity() > 0) {
                    p.setStockQuantity(p.getStockQuantity() - 1);
                    if (p.getNearExpiryQuantity() > 0) p.setNearExpiryQuantity(p.getNearExpiryQuantity() - 1);
                    productRepository.save(p);
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
        return enrichProductDto(productRepository.findById(id).orElseThrow(() -> new RuntimeException("Nu exista produs cu id-ul "+ id)));
    }

    //filtrare produse care necesita discount (expira)
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsExpiringBefore(LocalDate date)
    {
        List<Product> products=productRepository.findByExpirationDateBefore(date);
        //nu e nevoie de .orelsethrow deoarece va returna o lista goala, e ok, nu e null.
        return productMapper.toDtoList(products);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByBrandName(String brandName) {
        List<Product> products = productRepository.findByBrandName(brandName);
        return productMapper.toDtoList(products);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getProductsByCategoryName(String categoryName) {
        List<Product> products = productRepository.findByCategoryName(categoryName);
        return productMapper.toDtoList(products);
    }

    // SCRIERE

    public ProductResponseDTO createProduct(ProductCreationDTO creationDTO)
    {
        Product productToSave=productMapper.toEntity(creationDTO);
        //mapeza campurile simple (name,price,stock,...)

        // TRATAREA FK -> cautam Brand-ul si categoria produsului, daca nu exita aruncam exceptie.
        Brand brand = brandRepository.findById(creationDTO.getBrandId())
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + creationDTO.getBrandId()));
        Category category = categoryRepository.findById(creationDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + creationDTO.getCategoryId()));

        //daca le-a gasit, le setam in produs
        productToSave.setBrand(brand);
        productToSave.setCategory(category);

        // Postul efectiv in DB
        Product savedEntity=productRepository.save(productToSave);

        //returnam entitatea doar cu datele din DTO de raspuns catre controller
        return productMapper.toDto(savedEntity);
    }

    //UPDATE
    public ProductResponseDTO updateProduct(Long id, ProductCreationDTO updateDTO) {
        Product existingProduct = productRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Product not found for update with id: " + id)
        );
        //actualizam produsul existent, cu datele din DTO primit
        existingProduct.setName(updateDTO.getName());
        existingProduct.setPrice(updateDTO.getPrice());
        existingProduct.setStockQuantity(updateDTO.getStockQuantity());
        existingProduct.setUnitOfMeasure(updateDTO.getUnitOfMeasure());
        existingProduct.setExpirationDate(updateDTO.getExpirationDate());

       //actualizam relatiile FK

        //1. BRAND
        if(!existingProduct.getBrand().getId().equals(updateDTO.getBrandId()))
        {
            Brand newBrand = brandRepository.findById(updateDTO.getBrandId())
                    .orElseThrow(() -> new RuntimeException("Brand not found with id: " + updateDTO.getBrandId()));
            //cauta noul brand, daca nu il gaseste arunca exceptie
            //daca il gaseste, ajunge pana aici si il seteaza in produsul existent
            existingProduct.setBrand(newBrand);
        }

        //2. CATEGORIE -> exact acelasi concept ca pt BRAND
        if (!existingProduct.getCategory().getId().equals(updateDTO.getCategoryId())) {
            Category newCategory = categoryRepository.findById(updateDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + updateDTO.getCategoryId()));
            existingProduct.setCategory(newCategory);
        }
        // aici are loc put-ul efectiv
        Product updatedEntity = productRepository.save(existingProduct);

        return productMapper.toDto(updatedEntity);
        //returnam un dto doar cu campurile pt result ca confirmare
    }



        //DELETE

        public ProductResponseDTO deleteProduct(Long id)
        {
            Product p=productRepository.findById(id).orElseThrow(
                    () -> new RuntimeException("Product not found with id: "+ id)
            );
            //aici are loc DELETE-UL efectiv
            productRepository.delete(p);
            return productMapper.toDto(p);
            //returnam entitatea stearsa ca confirmare ca dto

        }
    }



