package com.annotation.service;

import com.annotation.model.Utilisateur;
import com.annotation.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Utilisateur user = utilisateurRepository.findByLogin(login)
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + login));

        var authorities = user.getRoles().stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getNomRole()))
            .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
            user.getLogin(), user.getPassword(), user.isActive(),
            true, true, true, authorities
        );
    }
}
