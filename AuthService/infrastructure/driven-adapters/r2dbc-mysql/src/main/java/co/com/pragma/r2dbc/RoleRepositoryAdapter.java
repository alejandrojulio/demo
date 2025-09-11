package co.com.pragma.r2dbc;

import co.com.pragma.model.role.Role;
import co.com.pragma.model.role.gateways.RoleRepository;
import co.com.pragma.r2dbc.mapper.RoleEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleReactiveRepository repository;

    @Override
    public Mono<Role> findById(Integer id) {
        return repository.findById(id)
                .map(RoleEntityMapper::fromEntity);
    }

    @Override
    public Mono<Role> findByName(String name) {
        return repository.findByName(name)
                .map(RoleEntityMapper::fromEntity);
    }

    @Override
    public Flux<Role> findAllActive() {
        return repository.findByActiveTrue()
                .map(RoleEntityMapper::fromEntity);
    }

    @Override
    public Flux<Role> findAll() {
        return repository.findAll()
                .map(RoleEntityMapper::fromEntity);
    }
}
