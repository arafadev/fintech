package com.fintech.fintech.role.service;

import com.fintech.fintech.response.Response;
import com.fintech.fintech.role.entity.Role;

import java.util.List;

public interface RoleService {

    Response<Role> createRole(Role roleRequest);

    Response<Role> updateRole(Role roleRequest);

    Response<List<Role>> getAllRoles();

    Response<?> deleteRole(Long id);

}
