package br.com.eric.usermanagement.repository;

import br.com.eric.usermanagement.domain.entity.Address;
import br.com.eric.usermanagement.domain.entity.User;
import br.com.eric.usermanagement.dto.UserFilter;
import br.com.eric.usermanagement.dto.UserSummaryResponse;
import br.com.eric.usermanagement.exception.InvalidQueryException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserSearchRepositoryImpl implements UserSearchRepository {

    private static final List<String> SORTABLE = List.of("name", "email", "status", "role", "createdAt", "city");

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<UserSummaryResponse> search(UserFilter filter, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<UserSummaryResponse> query = cb.createQuery(UserSummaryResponse.class);
        Root<User> user = query.from(User.class);
        Join<User, Address> main = joinMainAddress(cb, user);
        query.select(cb.construct(UserSummaryResponse.class,
                        user.get("id"), user.get("name"), user.get("email"), user.get("phone"),
                        user.get("role"), user.get("status"), main.get("city"), main.get("state")))
                .where(predicates(cb, user, main, filter))
                .orderBy(orders(cb, user, main, pageable.getSort()));

        List<UserSummaryResponse> content = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<User> countUser = countQuery.from(User.class);
        Join<User, Address> countMain = joinMainAddress(cb, countUser);
        countQuery.select(cb.count(countUser)).where(predicates(cb, countUser, countMain, filter));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private Join<User, Address> joinMainAddress(CriteriaBuilder cb, Root<User> user) {
        Join<User, Address> main = user.join("addresses", JoinType.LEFT);
        main.on(cb.isTrue(main.<Boolean>get("mainAddress")), cb.isFalse(main.<Boolean>get("deleted")));
        return main;
    }

    private Predicate[] predicates(CriteriaBuilder cb, Root<User> user, Join<User, Address> main, UserFilter filter) {
        List<Predicate> predicates = new ArrayList<>();
        if (StringUtils.hasText(filter.search())) {
            predicates.add(cb.or(
                    contains(cb, user.<String>get("name"), filter.search()),
                    contains(cb, user.<String>get("email"), filter.search())));
        }
        if (StringUtils.hasText(filter.city())) {
            predicates.add(contains(cb, main.<String>get("city"), filter.city()));
        }
        if (filter.status() != null) {
            predicates.add(cb.equal(user.get("status"), filter.status()));
        }
        if (filter.role() != null) {
            predicates.add(cb.equal(user.get("role"), filter.role()));
        }
        return predicates.toArray(new Predicate[0]);
    }

    private Predicate contains(CriteriaBuilder cb, Expression<String> expression, String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return cb.like(cb.lower(expression), "%" + escaped + "%", '\\');
    }

    private List<Order> orders(CriteriaBuilder cb, Root<User> user, Join<User, Address> main, Sort sort) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order requested : sort) {
            Expression<?> expression = switch (requested.getProperty()) {
                case "name" -> cb.lower(user.<String>get("name"));
                case "email" -> cb.lower(user.<String>get("email"));
                case "city" -> cb.lower(main.<String>get("city"));
                case "status", "role", "createdAt" -> user.get(requested.getProperty());
                default -> throw new InvalidQueryException("Campo de ordenação inválido: "
                        + requested.getProperty() + ". Permitidos: " + String.join(", ", SORTABLE));
            };
            orders.add(requested.isAscending() ? cb.asc(expression) : cb.desc(expression));
        }
        orders.add(cb.asc(user.get("id")));
        return orders;
    }
}