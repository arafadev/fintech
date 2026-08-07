package com.fintech.fintech.role.service;

import com.fintech.fintech.common.i18n.Messages;
import com.fintech.fintech.exception.ConflictException;
import com.fintech.fintech.exception.NotFoundException;
import com.fintech.fintech.response.Response;
import com.fintech.fintech.role.entity.Role;
import com.fintech.fintech.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final Messages messages;

    @Override
    public Response<Role> createRole(Role roleRequest) {
        if (roleRepository.findByName(roleRequest.getName()).isPresent()) {
            throw new ConflictException("error.role.exists");
        }
        return Response.ok(messages.get("success.role_saved"), roleRepository.save(roleRequest));
    }

    @Override
    public Response<Role> updateRole(Role roleRequest) {
        Role role = roleRepository.findById(roleRequest.getId())
                .orElseThrow(() -> new NotFoundException("error.role.not_found"));
        role.setName(roleRequest.getName());
        return Response.ok(messages.get("success.role_updated"), roleRepository.save(role));
    }

    @Override
    public Response<List<Role>> getAllRoles() {
        return Response.ok(messages.get("success.roles_retrieved"), roleRepository.findAll());
    }

    @Override
    public Response<?> deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new NotFoundException("error.role.not_found");
        }
        roleRepository.deleteById(id);
        return Response.ok(messages.get("success.role_deleted"));
    }
}
