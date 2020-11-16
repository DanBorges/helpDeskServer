package dani.daniel.helpDesk.api.security.jwt;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import dani.daniel.helpDesk.api.User;
import dani.daniel.helpDesk.api.enums.ProfileEnum;

public class JwtUserFactory {

	private JwtUserFactory() {
		
	}
	
	public static JwtUser create(User user) {
		return new JwtUser(user.getId(), 
							user.getEmail(), 
							user.getPassword(), 
							mapGranredAuthorities(user.getProfile()));
		
	}
	
	
	private static List<GrantedAuthority> mapGranredAuthorities(ProfileEnum profileEnum) {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority(profileEnum.toString()));
		return authorities;
	}
}
