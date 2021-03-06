package com.foodtech.blog.user.service;

import com.foodtech.blog.auth.exceptions.AuthException;
import com.foodtech.blog.auth.exceptions.NotAccessException;
import com.foodtech.blog.auth.service.AuthService;
import com.foodtech.blog.base.api.request.SearchRequest;
import com.foodtech.blog.base.api.response.SearchResponse;
import com.foodtech.blog.base.service.EmailSenderService;
import com.foodtech.blog.user.api.request.RegistrationRequest;
import com.foodtech.blog.user.api.request.UserRequest;
import com.foodtech.blog.user.exeception.UserExistException;
import com.foodtech.blog.base.api.model.UserDoc;
import com.foodtech.blog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserApiService {
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final AuthService authService;
    private final EmailSenderService emailSenderService;

    public UserDoc registration(RegistrationRequest request) throws UserExistException {
        if(userRepository.findByEmail(request.getEmail()).isPresent() == true){
            throw new UserExistException();
        }
        UserDoc userDoc = new UserDoc();
        userDoc.setEmail(request.getEmail());
        userDoc.setPassword(UserDoc.hexPassword(request.getPassword()));
        userDoc = userRepository.save(userDoc);

        emailSenderService.sendEmailRegistration(request.getEmail());

        return  userDoc;
    }

    public Optional<UserDoc> findByID(ObjectId id){
        return userRepository.findById(id);
    }
    public SearchResponse<UserDoc> search(
             SearchRequest request
    ){
        Criteria criteria = new Criteria();
        if(request.getQuery() != null && request.getQuery()!=""){
            criteria = criteria.orOperator(
                    Criteria.where("firstName").regex(request.getQuery(), "i"),
                    Criteria.where("lastName").regex(request.getQuery(), "i"),
                    Criteria.where("email").regex(request.getQuery(), "i")
            );
        }

        Query query = new Query(criteria);
        Long count = mongoTemplate.count(query, UserDoc.class);
        query.limit(request.getSize());
        query.skip(request.getSkip());

        List<UserDoc> userDocs = mongoTemplate.find(query, UserDoc.class);
        return SearchResponse.of(userDocs, count);
    }

    public UserDoc update(UserRequest request) throws  AuthException {
        UserDoc userDoc =authService.currentUser();

        userDoc.setFirstName(request.getFirstName());
        userDoc.setLastName(request.getLastName());
        userDoc.setAddress(request.getAddress());
        userDoc.setCompany(request.getCompany());

        userRepository.save(userDoc);

        return userDoc;
    }

    public void delete(ObjectId id) throws NotAccessException, AuthException {
        if(authService.currentUser().getId().equals(id) == false) throw new NotAccessException();
        userRepository.deleteById(id);
    }
}
