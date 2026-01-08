package covrig.eduard.project.Services;

import covrig.eduard.project.Models.Role;
import covrig.eduard.project.Models.User;
import covrig.eduard.project.Repositories.UserRepository;
import covrig.eduard.project.dtos.user.UserProfileUpdateDTO;
import covrig.eduard.project.dtos.user.UserRegistrationDTO;
import covrig.eduard.project.dtos.user.UserResponseDTO;
import covrig.eduard.project.mappers.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    //metode de citire (READ)
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers()
    {
        return userMapper.toDtoList(userRepository.findAll());
    }


    @Transactional(readOnly=true)
    public UserResponseDTO getUserById(Long id)
    {
        User user=userRepository.findById(id).orElseThrow(() -> new RuntimeException("Nu exista user cu id " + id));
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit."));
        return userMapper.toDto(user);
    }

    public UserResponseDTO updateProfile(String email, UserProfileUpdateDTO dto)
    {
        User user=userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit."));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) { //doar daca e, pt ca nu are notblank in dto
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) { //doar daca e, pt ca nu are notblank in dto
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        return userMapper.toDto(userRepository.save(user));
    }


    //CREATE
    public UserResponseDTO createUser(UserRegistrationDTO userRegistrationDTO)
    {
        //validare date
        if(userRepository.existsByEmail(userRegistrationDTO.getEmail()))
            throw new RuntimeException("Exista deja un cont cu adresa de email " + userRegistrationDTO.getEmail() + " inregistrat.");
        User userToCreate=userMapper.toEntity(userRegistrationDTO); //aici e id e null
        userToCreate.setPasswordHash(passwordEncoder.encode(userRegistrationDTO.getPassword()));
        userToCreate.setRole(Role.USER);
        userToCreate.setCreatedAt(Instant.now());
        User savedUser=userRepository.save(userToCreate); //salvam in db, si ii da automat si campul de id.
        return userMapper.toDto(savedUser);

    }

    // Update & delete
    public UserResponseDTO updateUser(Long id,UserRegistrationDTO dto)
    {
        //validare date
        User user=userRepository.findById(id).orElseThrow(() -> new RuntimeException("Nu exista utilizatorul cu id "+ id));
        if(userRepository.existsByEmail(dto.getEmail())&&
        !user.getEmail().equalsIgnoreCase(dto.getEmail()))
            throw new RuntimeException("Email-ul " + dto.getEmail() + " este deja utilizat.");
        //daca totul e ok, ajungem aici si facem update-ul efectiv

        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());

        if(dto.getPassword()!=null && !dto.getPassword().isBlank())
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        //.save returneaza mereu la final obiectul salvat.
        return userMapper.toDto(userRepository.save(user));
    }
    public UserResponseDTO deleteUser(Long id)
    {
        User user=userRepository.findById(id).orElseThrow(() -> new RuntimeException("Nu exista utilizatorul cu id "+ id));
        userRepository.delete(user); //aici nu putem face inline totul ca la update, deoarece .delete returneaza void nu entitatea.
        return userMapper.toDto(user);

    }
}
