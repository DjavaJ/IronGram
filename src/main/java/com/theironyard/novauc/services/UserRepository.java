package com.theironyard.novauc.services;

import com.theironyard.novauc.entities.User;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by dangelojoyce on 3/21/17.
 */
public interface UserRepository extends CrudRepository<User, Integer> {

    User findFirstByName(String name);


}
