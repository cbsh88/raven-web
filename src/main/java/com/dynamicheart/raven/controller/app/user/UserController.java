package com.dynamicheart.raven.controller.app.user;

import com.dynamicheart.raven.authorization.annotation.Authorization;
import com.dynamicheart.raven.authorization.annotation.CurrentUser;
import com.dynamicheart.raven.authorization.manager.TokenManager;
import com.dynamicheart.raven.authorization.model.TokenModel;
import com.dynamicheart.raven.constant.Message;
import com.dynamicheart.raven.controller.app.user.field.CreateUserForm;
import com.dynamicheart.raven.controller.app.user.field.UpdateUserForm;
import com.dynamicheart.raven.controller.app.user.field.UserInfoFields;
import com.dynamicheart.raven.controller.app.user.populator.CreateUserFormPopulator;
import com.dynamicheart.raven.controller.app.user.populator.UpdateUserFormPopulator;
import com.dynamicheart.raven.controller.app.user.populator.UserInfoFieldsPopulator;
import com.dynamicheart.raven.controller.common.model.ErrorResponse;
import com.dynamicheart.raven.model.user.User;
import com.dynamicheart.raven.services.user.UserService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.inject.Inject;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Inject
    private UserService userService;

    @Inject
    private TokenManager tokenManager;

    @Inject
    private UserInfoFieldsPopulator infoFieldsPopulator;

    @Inject
    private CreateUserFormPopulator createUserFormPopulator;

    @Inject
    private UpdateUserFormPopulator updateUserFormPopulator;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @Authorization
    @ApiResponses({
            @ApiResponse(code = 200, response = UserInfoFields.class, message = "Delete user")
    })
    ResponseEntity<?> get(@PathVariable String id, @CurrentUser @ApiIgnore User currentUser) throws Exception{
        if(!id.equals(currentUser.getId())){
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_FORBIDDEN), HttpStatus.FORBIDDEN);
        }

        UserInfoFields userInfoFields = infoFieldsPopulator.populate(currentUser);

        return new ResponseEntity<>(userInfoFields, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    @ApiResponses({
            @ApiResponse(code = 201, response = TokenModel.class, message = "Create a user")
    })
    ResponseEntity<?> post(@RequestBody CreateUserForm createUserForm) throws Exception{

        User user = createUserFormPopulator.populate(createUserForm);

        //TODO check duplicate

        user = userService.create(user);

        TokenModel token = tokenManager.createToken(user.getId());
        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @Authorization
    @ApiResponses({
            @ApiResponse(code = 200, response = UserInfoFields.class, message = "Update user")
    })
    ResponseEntity<?> put(@PathVariable String id, @CurrentUser @ApiIgnore User currentUser, @RequestBody UpdateUserForm updateUserForm) throws Exception{
        if(!id.equals(currentUser.getId())){
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_FORBIDDEN), HttpStatus.FORBIDDEN);
        }

        currentUser = updateUserFormPopulator.populate(updateUserForm);
        currentUser = userService.save(currentUser);

        UserInfoFields userInfoFields = infoFieldsPopulator.populate(currentUser);

        return new ResponseEntity<>(userInfoFields, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @Authorization
    @ApiResponses({
            @ApiResponse(code = 200, response = UserInfoFields.class, message = "Delete user")
    })
    ResponseEntity<?> delete(@PathVariable String id, @CurrentUser @ApiIgnore User currentUser) throws Exception{
        if(!id.equals(currentUser.getId())){
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_FORBIDDEN), HttpStatus.FORBIDDEN);
        }

        userService.delete(currentUser);

        UserInfoFields userInfoFields = infoFieldsPopulator.populate(currentUser);

        return new ResponseEntity<>(userInfoFields, HttpStatus.OK);
    }
}
