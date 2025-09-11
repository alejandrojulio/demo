package co.com.pragma.r2dbc;

import co.com.pragma.r2dbc.entity.RoleEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface RoleReactiveRepository extends ReactiveCrudRepository<RoleEntity, Integer> {
    Mono<RoleEntity> findByName(String name);
    Flux<RoleEntity> findByActiveTrue();
}
