package co.com.pragma.usecase.user;

import co.com.pragma.model.user.gateways.ApplicationLogger;
import co.com.pragma.model.user.User;
import co.com.pragma.model.user.gateways.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@Service
public class UserUseCase {

    private final UserRepository userRepository;
    private final ApplicationLogger logger;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );

    private static final BigDecimal MIN_SALARY = BigDecimal.ZERO;
    private static final BigDecimal MAX_SALARY = new BigDecimal("15000000");

    public UserUseCase(UserRepository userRepository, ApplicationLogger logger) {
        this.userRepository = userRepository;
        this.logger = logger;
    }

    public Mono<User> getuser(String id) {
        return userRepository.findById(id)
                .doOnSubscribe(s -> logger.info("Buscando usuario con ID: {}", id))
                .doOnSuccess(user -> {
                    if (user != null) {
                        logger.info("Usuario encontrado con ID: {}", id);
                    } else {
                        logger.info("Usuario no encontrado con ID: {}", id);
                    }
                })
                .doOnError(error -> logger.error("Error al buscar usuario con ID: " + id, error));
    }

    // ✅ AHORA DEBERÍA FUNCIONAR: @Transactional con configuración estándar
    @Transactional
    public Mono<User> createUser(User userToCreate) {
        logger.info("Iniciando proceso de creación de usuario para email: {}", userToCreate.getEmail());

        return Mono.just(userToCreate)
                .doOnNext(user -> logger.info("Validando datos del usuario: {}", user.getEmail()))
                .flatMap(this::validateRequiredFields)
                .flatMap(this::validateEmailFormat)
                .flatMap(this::validateSalaryRange)
                .flatMap(this::ensureEmailIsUnique)
                .map(this::enrichUserData)
                .flatMap(userRepository::save) // ← Operación transaccional
                .doOnSuccess(savedUser -> logger.info("Usuario creado exitosamente con ID: {}", savedUser.getId()))
                .doOnError(error -> logger.error("Error en la creación de usuario para email: " + userToCreate.getEmail(), error));
    }

    private Mono<User> validateRequiredFields(User user) {
        logger.info("Validando campos obligatorios para usuario: {}", user.getEmail());

        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("El nombre es obligatorio"));
        }

        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("El apellido es obligatorio"));
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("El correo electrónico es obligatorio"));
        }

        if (user.getBaseSalary() == null) {
            return Mono.error(new IllegalArgumentException("El salario base es obligatorio"));
        }

        logger.info("Campos obligatorios validados correctamente para: {}", user.getEmail());
        return Mono.just(user);
    }

    private Mono<User> validateEmailFormat(User user) {
        logger.info("Validando formato de email para: {}", user.getEmail());

        if (!EMAIL_PATTERN.matcher(user.getEmail().trim()).matches()) {
            return Mono.error(new IllegalArgumentException("El formato del correo electrónico no es válido"));
        }

        logger.info("Formato de email válido para: {}", user.getEmail());
        return Mono.just(user);
    }

    private Mono<User> validateSalaryRange(User user) {
        logger.info("Validando rango de salario para usuario: {} - Salario: {}", user.getEmail(), user.getBaseSalary());

        BigDecimal salary = user.getBaseSalary();

        if (salary.compareTo(MIN_SALARY) < 0) {
            return Mono.error(new IllegalArgumentException("El salario base no puede ser menor a 0"));
        }

        if (salary.compareTo(MAX_SALARY) > 0) {
            return Mono.error(new IllegalArgumentException("El salario base no puede ser mayor a 15,000,000"));
        }

        logger.info("Rango de salario válido para usuario: {}", user.getEmail());
        return Mono.just(user);
    }

    private Mono<User> ensureEmailIsUnique(User user) {
        logger.info("Verificando unicidad de email: {}", user.getEmail());

        return userRepository.existsByEmail(user.getEmail().toLowerCase().trim())
                .flatMap(emailExists -> {
                    if (emailExists) {
                        logger.warn("Intento de registro con email ya existente: {}", user.getEmail());
                        return Mono.error(new IllegalArgumentException("El correo electrónico ya está registrado: " + user.getEmail()));
                    }
                    logger.info("Email único confirmado para: {}", user.getEmail());
                    return Mono.just(user);
                })
                .doOnError(error -> {
                    if (!(error instanceof IllegalArgumentException)) {
                        logger.error("Error al verificar unicidad del email: " + user.getEmail(), error);
                    }
                });
    }

    private User enrichUserData(User user) {
        logger.info("Enriqueciendo datos del usuario: {}", user.getEmail());

        return user.toBuilder()
                .email(user.getEmail().toLowerCase().trim())
                .firstName(user.getFirstName().trim())
                .lastName(user.getLastName().trim())
                .address(user.getAddress() != null ? user.getAddress().trim() : null)
                .phone(user.getPhone() != null ? user.getPhone().trim() : null)
                .build();
    }
}