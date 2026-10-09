package br.com.eric.usermanagement.repository;

import br.com.eric.usermanagement.dto.UserFilter;
import br.com.eric.usermanagement.dto.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserSearchRepository {
    Page<UserSummaryResponse> search(UserFilter filter, Pageable pageable);
}
