package com.dynamicheart.raven.controller.app.member;

import com.dynamicheart.raven.authorization.annotation.Authorization;
import com.dynamicheart.raven.authorization.annotation.CurrentUser;
import com.dynamicheart.raven.constant.Constants;
import com.dynamicheart.raven.constant.Message;
import com.dynamicheart.raven.controller.app.member.field.MemberInfoFields;
import com.dynamicheart.raven.controller.app.member.populator.MemberInfoFieldsPopulator;
import com.dynamicheart.raven.controller.common.model.ErrorResponse;
import com.dynamicheart.raven.model.house.House;
import com.dynamicheart.raven.model.member.Member;
import com.dynamicheart.raven.model.user.User;
import com.dynamicheart.raven.services.house.HouseService;
import com.dynamicheart.raven.services.member.MemberService;
import com.dynamicheart.raven.services.user.UserService;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.inject.Inject;

@RestController
@RequestMapping("/api/v1/houses/{houseId}/members")
public class MemberController {

    @Inject
    private HouseService houseService;

    @Inject
    private MemberService memberService;

    @Inject
    private UserService userService;

    @Inject
    private MemberInfoFieldsPopulator memberInfoFieldsPopulator;

    @RequestMapping(value = "/{userId}", method = RequestMethod.GET)
    @Authorization
    @ApiResponses({
            @ApiResponse(code = 200, response = MemberInfoFields.class, message = "Get member info")
    })
    ResponseEntity<?> get(@PathVariable String houseId,
                          @PathVariable String userId,
                          @CurrentUser @ApiIgnore User currentUser) throws Exception {
        House house = houseService.getById(houseId);
        if (house == null) {
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_NOT_FOUND), HttpStatus.NOT_FOUND);
        }

        Member member = memberService.findTopByHouseIdAndUser(house.getId(), currentUser);

        if (!house.getPublicity() || member == null) {
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_NOT_FOUND), HttpStatus.NOT_FOUND);
        }

        MemberInfoFields memberInfoFields = memberInfoFieldsPopulator.populate(member);

        return new ResponseEntity<>(memberInfoFields, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    @Authorization
    @ApiResponses({
            @ApiResponse(code = 201, response = MemberInfoFields.class, message = "Add a member")
    })
    ResponseEntity<?> post(@PathVariable String houseId,
                           @RequestBody String userId,
                           @CurrentUser @ApiIgnore User currentUser) throws Exception{
        Member currentUserMember = memberService.findTopByHouseIdAndUser(houseId, currentUser);
        if (currentUserMember == null || !currentUserMember.getRole().equals(Constants.MEMBER_ROLE_LORD)) {
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_FORBIDDEN), HttpStatus.FORBIDDEN);
        }

        if (memberService.findTopByHouseIdAndUser(houseId, currentUser) != null) {
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_NOT_FOUND), HttpStatus.BAD_REQUEST);
        }

        House house = houseService.getById(houseId);
        if (house == null) {
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_NOT_FOUND), HttpStatus.NOT_FOUND);
        }

        User user = userService.getById(userId);
        if (user == null) {
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_NOT_FOUND), HttpStatus.NOT_FOUND);
        }

        Member member = new Member();
        member.setHouseId(houseId);
        member.setUser(user);

        member = memberService.create(member);

        MemberInfoFields memberInfoFields = memberInfoFieldsPopulator.populate(member);

        return new ResponseEntity<>(memberInfoFields, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.PUT)
    @Authorization
    @ApiResponses({
            @ApiResponse(code = 200, response = MemberInfoFields.class, message = "Update the role of member")
    })
    ResponseEntity<?> put(@PathVariable String houseId,
                          @PathVariable String userId,
                          @RequestBody Integer role,
                          @CurrentUser @ApiIgnore User currentUser) throws Exception{
        Member currentUserMember = memberService.findTopByHouseIdAndUser(houseId, currentUser);
        if (currentUserMember == null || !currentUserMember.getRole().equals(Constants.MEMBER_ROLE_LORD)) {
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_FORBIDDEN), HttpStatus.FORBIDDEN);
        }

        if(userId.equals(currentUser.getId())){
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_FORBIDDEN), HttpStatus.FORBIDDEN);
        }

        House house = houseService.getById(houseId);
        User user = userService.getById(userId);
        if(user == null || house == null){
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_NOT_FOUND), HttpStatus.NOT_FOUND);
        }

        Member member = memberService.findTopByHouseIdAndUser(houseId, user);

        if(role.equals(Constants.MEMBER_ROLE_LORD)){
            member.setRole(Constants.MEMBER_ROLE_LORD);
            currentUserMember.setRole(Constants.MEMBER_ROLE_ORDINARY);
            memberService.save(currentUserMember);
        }else {
            member.setRole(role);
        }

        member = memberService.save(member);
        MemberInfoFields memberInfoFields = memberInfoFieldsPopulator.populate(member);

        return new ResponseEntity<>(memberInfoFields, HttpStatus.OK);
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.DELETE)
    @Authorization
    @ApiResponses({
            @ApiResponse(code = 200, response = MemberInfoFields.class, message = "Delete a member")
    })
    ResponseEntity<?> delete(@PathVariable String houseId,
                             @PathVariable String userId,
                             @CurrentUser @ApiIgnore User currentUser) throws Exception {
        Member currentUserMember = memberService.findTopByHouseIdAndUser(houseId, currentUser);
        Boolean isHouseLord = currentUserMember != null && currentUserMember.getRole().equals(Constants.MEMBER_ROLE_LORD);
        Boolean isCurrentUser = userId.equals(currentUser.getId());
        if (!isHouseLord && !isCurrentUser) {
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_FORBIDDEN), HttpStatus.FORBIDDEN);
        }

        House house = houseService.getById(houseId);
        if (house == null) {
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_NOT_FOUND), HttpStatus.NOT_FOUND);
        }

        Member member = memberService.findTopByHouseIdAndUser(houseId, currentUser);
        if (member == null) {
            return new ResponseEntity<>(new ErrorResponse(Message.MESSAGE_NOT_FOUND), HttpStatus.NOT_FOUND);
        }

        memberService.delete(member);

        //TODO inconsistent
        house.setMemberNumbers(house.getMemberNumbers() - 1);

        MemberInfoFields memberInfoFields = memberInfoFieldsPopulator.populate(member);

        return new ResponseEntity<>(memberInfoFields, HttpStatus.OK);
    }
}
