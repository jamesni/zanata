package net.openl10n.flies.service.impl;

import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import net.openl10n.flies.common.LocaleId;
import net.openl10n.flies.dao.PersonDAO;
import net.openl10n.flies.dao.SupportedLanguageDAO;
import net.openl10n.flies.exception.FliesException;
import net.openl10n.flies.model.HPerson;
import net.openl10n.flies.model.HSupportedLanguage;
import net.openl10n.flies.service.LanguageTeamService;

@Name("languageTeamServiceImpl")
@AutoCreate
@Scope(ScopeType.STATELESS)
public class LanguageTeamServiceImpl implements LanguageTeamService
{
   @In
   PersonDAO personDAO;
   
   @In
   SupportedLanguageDAO supportedLanguageDAO;
   

   public List<HSupportedLanguage> getLanguageMemberships(String userName)
   {
      return personDAO.getLanguageMemberships(userName);
   }

   public boolean joinLanguageTeam(String locale, Long personId) throws FliesException
   {
      HSupportedLanguage lang = supportedLanguageDAO.findByLocaleId(new LocaleId(locale));
      HPerson currentPerson = personDAO.findById(personId, false);

      if (!lang.getMembers().contains(currentPerson))
      {
         if (currentPerson.getTribeMemberships().size() >= MAX_NUMBER_MEMBERSHIP)
         {
            throw new FliesException("You can only be a member of up to " + MAX_NUMBER_MEMBERSHIP + " languages at one time.");
         }
         else
         {
            lang.getMembers().add(currentPerson);
            supportedLanguageDAO.flush();
            return true;
         }
      }
      return false;
   }

   public boolean leaveLanguageTeam(String locale, Long personId)
   {
      HSupportedLanguage lang = supportedLanguageDAO.findByLocaleId(new LocaleId(locale));
      HPerson currentPerson = personDAO.findById(personId, false);

      if (lang.getMembers().contains(currentPerson))
      {
         lang.getMembers().remove(currentPerson);
         supportedLanguageDAO.flush();
         return true;
      }

      return false;

   }
}