package com.nephest.battlenet.sc2.donations.repository;

import com.nephest.battlenet.sc2.donations.model.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long>
{

}
