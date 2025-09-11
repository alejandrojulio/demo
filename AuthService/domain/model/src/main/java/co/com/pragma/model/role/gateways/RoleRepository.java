package co.com.pragma.model.role.gateways;

import co.com.pragma.model.role.Role;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoleRepository {
    Mono<Role> findById(Integer id);
    Mono<Role> findByName(String name);
    Flux<Role> findAllActive();
    Flux<Role> findAll();
}
