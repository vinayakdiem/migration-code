package com.diemlife.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.diemlife.models.Address;
import com.diemlife.models.Brand;


@Repository
public class BrandDAO extends TypedSingletonDAO<Brand> {

	@PersistenceContext
	private EntityManager em;

	public Brand getBrand(String name) {
		return em.createQuery("Select c from Company c where c.name = :name", Brand.class)
				.setParameter("name", name)
				.getResultList()
				.stream()
				.findFirst()
				.orElse(null);
	}

	public Brand getBrandFromId(Long companyId) {
		return em.createQuery("Select c from Company c where c.id = :id", Brand.class)
				.setParameter("id", companyId)
				.getResultList()
				.stream()
				.findFirst()
				.orElse(null);
	}

	// If create is called for an existing item, this method returns null
	public Brand createBrand(String name, String orgType, String website) {
		final Brand brand = new Brand();
		brand.setName(name);
		brand.setOrgType(orgType);
		brand.setWebsite(website);

		return save(brand, Brand.class);
	}


	public boolean addAddressForBrand(Address address, Integer companyId) {
		Brand brand = load(companyId.longValue(), Brand.class);
		if (brand == null) {
			return false;
		}
		brand.setAddress(address);
		save(brand, Brand.class);
		return true;
	}

	public boolean addPhoneForBrand(String phone, Integer companyId) {
		Brand brand = load(companyId.longValue(), Brand.class);
		if (brand == null) {
			return false;
		}
		brand.setPhone(phone);
		save(brand, Brand.class);
		return true;
	}
}
