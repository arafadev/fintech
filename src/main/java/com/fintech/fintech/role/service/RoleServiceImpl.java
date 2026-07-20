package com.fintech.fintech.role.service;

import com.fintech.fintech.exception.BadRequestException;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.role.entity.Role;
import com.fintech.fintech.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements  RoleService{

    private final RoleRepository roleRepository;

    @Override
    public Response<Role> createRole(Role roleRequest) {
        if(roleRepository.findByName(roleRequest.getName()).isPresent()){
            throw new BadRequestException("Role already exists");
        }
        Role savedRole = roleRepository.save(roleRequest);

        return Response.<Role>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role Saved Successfully")
                .data(savedRole)
                .build();
    }

    @Override
    public Response<Role> updateRole(Role roleRequest) {
        Role role = roleRepository.findById(roleRequest.getId()).orElseThrow(()->new NotFoundException("Role not found"));

        role.setName(roleRequest.getName());
        Role updatedRole = roleRepository.save(role);

        return Response.<Role>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role Updated Successfully")
                .data(updatedRole)
                .build();
    }

    @Override
    public Response<List<Role>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        return Response.<List<Role>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Roles Retrieved Successfully")
                .data(roles)
                .build();
    }

    @Override
    public Response<?> deleteRole(Long id) {
        if(!roleRepository.existsById(id)){
            throw  new NotFoundException("Role not found");
        }
        roleRepository.deleteById(id);

       return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Role deleted successfully")
                .build();


    }
}
