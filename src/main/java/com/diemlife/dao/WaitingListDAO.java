package dao;

import models.WaitingList;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.Date;

public class WaitingListDAO {

  public void persist(WaitingList transientInstance, EntityManager entityManager) {

    EntityTransaction tx = null;
    try {
      tx = entityManager.getTransaction();
      tx.begin();

      entityManager.persist(transientInstance);

      tx.commit();
    }
    catch (RuntimeException e) {
      if ( tx != null && tx.isActive() ) tx.rollback();
      throw e; // or display error message
    }
  }

  public void remove(WaitingList persistentInstance, EntityManager entityManager) {

    EntityTransaction tx = null;
    try {
      tx = entityManager.getTransaction();
      tx.begin();

      entityManager.remove(persistentInstance);

      tx.commit();
    }
    catch (RuntimeException e) {
      if ( tx != null && tx.isActive() ) tx.rollback();
      throw e; // or display error message
    }
  }

  public static void addNewUserToWaitingList(WaitingList waitingListUser, EntityManager em) {

    try {
      Date date = new Date();

      if (waitingListUser != null) {
        WaitingList waitingList = new WaitingList();
        waitingList.setUserName(waitingListUser.getUserName());
        waitingList.setEmail(waitingListUser.getEmail());
        waitingList.setZip(waitingListUser.getZip());
        waitingList.setHearAboutUs(waitingListUser.getHearAboutUs());
        waitingList.setCompanySize(waitingListUser.getCompanySize());
        waitingList.setAddedDate(date);
        waitingList.setLastModifiedDate(date);
        waitingList.setRsvp(waitingListUser.getRsvp());

        em.persist(waitingList);
      }
    } catch (Exception ex) {
      Logger.error("WaitingListDAO :: addNewUserToWaitingList :: Error => " + ex,ex);
    }
  }

}
