package co.com.pragma.r2dbc;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.gateways.UserRepository;
import co.com.pragma.r2dbc.entity.UserEntity;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import co.com.pragma.r2dbc.mapper.UserEntityMapper;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class UserRepositoryAdapter extends ReactiveAdapterOperations<
        User,
        UserEntity,
        String,
        UserReactiveRepository>
        implements UserRepository {

    public UserRepositoryAdapter(UserReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> UserEntityMapper.fromEntity((UserEntity) d));
    }

    @Override
    public Mono<User> findById(String id) {
        return super.findById(id);
    }

    @Override
    public Flux<User> findAll() {
        return super.findAll();
    }


    @Override
    public Mono<User> save(User user) {
        return super.save(user);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email){
        return repository.existsByEmail(email);
    }

    @Override
    public Mono<User> findByDocument(String document) {
        return repository.findByDocument(document)
                .map(UserEntityMapper::fromEntity);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return repository.findByEmail(email)
                .map(UserEntityMapper::fromEntity);
    }
}