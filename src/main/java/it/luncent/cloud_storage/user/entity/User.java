package it.luncent.cloud_storage.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Entity
@Table(name = "users")
@Getter
public class User {
    @Id
    private Long id;

    @Column(unique=true)
    @Size(min=5, max=20)
    private String username;
    private String password;
}
