package co.com.pragma.usecase.user;

import co.com.pragma.model.auth.gateways.PasswordEncoder;
import co.com.pragma.model.user.gateways.ApplicationLogger;
import co.com.pragma.model.user.User;
import co.com.pragma.model.user.UserDTO;
import co.com.pragma.model.user.UserResponseDTO;
import co.com.pragma.model.user.UserMapper;
import co.com.pragma.model.user.gateways.UserRepository;
import co.com.pragma.usecase.user.UserUseCaseConstants;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class UserUseCase {

    private final UserRepository userRepository;
    private final ApplicationLogger logger;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    private static final BigDecimal MIN_SALARY = BigDecimal.ZERO;
    private static final BigDecimal MAX_SALARY = new BigDecimal("15000000");

    public UserUseCase(UserRepository userRepository, ApplicationLogger logger, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.logger = logger;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<UserResponseDTO> getuser(String id) {
        return userRepository.findById(id)
                .doOnSubscribe(s -> logger.info(UserUseCaseConstants.LOG_SEARCHING_USER, id))
                .doOnSuccess(user -> {
                    if (user != null) {
                        logger.info(UserUseCaseConstants.LOG_USER_FOUND, id);
                    } else {
                        logger.info(UserUseCaseConstants.LOG_USER_NOT_FOUND, id);
                    }
                })
                .map(UserMapper::toResponseDTO)
                .doOnError(error -> logger.error(UserUseCaseConstants.LOG_ERROR_SEARCHING_USER + id, error));
    }

    public Mono<UserResponseDTO> createUser(UserDTO userDTO) {
        logger.info(UserUseCaseConstants.LOG_CREATING_USER, userDTO.getEmail());

        return Mono.just(userDTO)
                .map(UserMapper::fromDTO)
                .doOnNext(user -> logger.info(UserUseCaseConstants.LOG_VALIDATING_USER_DATA, user.getEmail()))
                .flatMap(this::validateRequiredFields)
                .flatMap(this::validateEmailFormat)
                .flatMap(this::validateSalaryRange)
                .flatMap(this::ensureEmailIsUnique)
                .map(this::encryptPasswordAndEnrichData)
                .flatMap(userRepository::save) // ← Operación transaccional
                .map(UserMapper::toResponseDTO)
                .doOnSuccess(savedUser -> logger.info(UserUseCaseConstants.LOG_USER_CREATED_SUCCESS, savedUser.getId()))
                .doOnError(error -> logger.error(UserUseCaseConstants.LOG_ERROR_CREATING_USER + userDTO.getEmail(), error));
    }

    private Mono<User> validateRequiredFields(User user) {
        logger.info(UserUseCaseConstants.LOG_VALIDATING_REQUIRED_FIELDS, user.getEmail());

        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(UserUseCaseConstants.ERROR_FIRST_NAME_REQUIRED));
        }

        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(UserUseCaseConstants.ERROR_LAST_NAME_REQUIRED));
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException(UserUseCaseConstants.ERROR_EMAIL_REQUIRED));
        }

        if (user.getBaseSalary() == null) {
            return Mono.error(new IllegalArgumentException(UserUseCaseConstants.ERROR_BASE_SALARY_REQUIRED));
        }

        logger.info(UserUseCaseConstants.LOG_REQUIRED_FIELDS_VALIDATED, user.getEmail());
        return Mono.just(user);
    }

    private Mono<User> validateEmailFormat(User user) {
        logger.info(UserUseCaseConstants.LOG_VALIDATING_EMAIL_FORMAT, user.getEmail());

        if (!EMAIL_PATTERN.matcher(user.getEmail().trim()).matches()) {
            return Mono.error(new IllegalArgumentException(UserUseCaseConstants.ERROR_INVALID_EMAIL_FORMAT));
        }

        logger.info(UserUseCaseConstants.LOG_EMAIL_FORMAT_VALID, user.getEmail());
        return Mono.just(user);
    }

    private Mono<User> validateSalaryRange(User user) {
        logger.info(UserUseCaseConstants.LOG_VALIDATING_SALARY_RANGE, user.getEmail(), user.getBaseSalary());

        BigDecimal salary = user.getBaseSalary();

        if (salary.compareTo(MIN_SALARY) < 0) {
            return Mono.error(new IllegalArgumentException(UserUseCaseConstants.ERROR_SALARY_BELOW_MIN));
        }

        if (salary.compareTo(MAX_SALARY) > 0) {
            return Mono.error(new IllegalArgumentException(UserUseCaseConstants.ERROR_SALARY_ABOVE_MAX));
        }

        logger.info(UserUseCaseConstants.LOG_SALARY_RANGE_VALID, user.getEmail());
        return Mono.just(user);
    }

    private Mono<User> ensureEmailIsUnique(User user) {
        logger.info(UserUseCaseConstants.LOG_CHECKING_EMAIL_UNIQUENESS, user.getEmail());

        return userRepository.existsByEmail(user.getEmail().toLowerCase().trim())
                .flatMap(emailExists -> {
                    if (emailExists) {
                        logger.warn(UserUseCaseConstants.LOG_EMAIL_ALREADY_EXISTS, user.getEmail());
                        return Mono.error(new IllegalArgumentException(UserUseCaseConstants.ERROR_EMAIL_ALREADY_REGISTERED + user.getEmail()));
                    }
                    logger.info(UserUseCaseConstants.LOG_EMAIL_UNIQUE_CONFIRMED, user.getEmail());
                    return Mono.just(user);
                })
                .doOnError(error -> {
                    if (!(error instanceof IllegalArgumentException)) {
                        logger.error(UserUseCaseConstants.LOG_ERROR_CHECKING_EMAIL_UNIQUENESS + user.getEmail(), error);
                    }
                });
    }

    /**
     * Encripta la contraseña y enriquece los datos del usuario
     */
    private User encryptPasswordAndEnrichData(User user) {
        logger.info("Encriptando contraseña para usuario: {}", user.getEmail());
        
        // Encriptar contraseña
        String encryptedPassword = passwordEncoder.encode(user.getPassword());
        
        // Enriquecer datos y establecer contraseña encriptada
        User enrichedUser = UserMapper.enrichUserData(user);
        
        return enrichedUser.toBuilder()
                .password(encryptedPassword)
                .build();
    }

    // Validación adicional para contraseña
    private Mono<User> validatePasswordRequirements(User user) {
        logger.info("Validando requisitos de contraseña para usuario: {}", user.getEmail());
        
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Password is required"));
        }
        
        if (user.getPassword().length() < 6) {
            return Mono.error(new IllegalArgumentException("Password must be at least 6 characters long"));
        }
        
        logger.info("Contraseña válida para usuario: {}", user.getEmail());
        return Mono.just(user);
    }
}