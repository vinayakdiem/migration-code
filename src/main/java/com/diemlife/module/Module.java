package com.diemlife.module;

import com.feth.play.module.mail.IMailer;
import com.feth.play.module.mail.Mailer;
import com.feth.play.module.mail.Mailer.MailerFactory;
import com.feth.play.module.pa.Resolver;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.diemlife.dao.FundraisingLinkDAO;
import net.sf.ehcache.CacheManager;
import play.Logger;
import play.inject.ApplicationLifecycle;
import com.diemlife.plugins.HibernateSearchPlugin;
import com.diemlife.plugins.S3Plugin;
import com.diemlife.providers.DIEMBasicAuthProvider;
import com.diemlife.providers.MyUsernamePasswordAuthProvider;
import com.diemlife.providers.RedisAuthProvider;
import com.diemlife.services.CommentsService;
import com.diemlife.services.ContentReportingService;
import com.diemlife.services.FormSecurityService;
import com.diemlife.services.FundraisingService;
import com.diemlife.services.LeaderboardService;
import com.diemlife.services.LinkPreviewService;
import com.diemlife.services.MyResolver;
import com.diemlife.services.NotificationService;
import com.diemlife.services.OutgoingEmailService;
import com.diemlife.services.PaymentTransactionFacade;
import com.diemlife.services.PlaidLinkService;
import com.diemlife.services.QuestMemberService;
import com.diemlife.services.QuestService;
import com.diemlife.services.ReportingService;
import com.diemlife.services.SeoService;
import com.diemlife.services.StripeConnectService;
import com.diemlife.services.TaskGroupService;
import com.diemlife.services.UserActivationService;
import com.diemlife.services.UserProvider;
import com.diemlife.services.UserPaymentFeeService;
import com.diemlife.utils.DAOProvider;
import com.diemlife.utils.FeeUtility;
import com.diemlife.utils.PDFTemplateStamper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

/**
 * Initial DI module.
 */
public class Module extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().implement(IMailer.class, Mailer.class).build(MailerFactory.class));
        bind(Resolver.class).to(MyResolver.class);
        //bind(HibernateUserServicePlugin.class).asEagerSingleton();
        bind(MyUsernamePasswordAuthProvider.class).asEagerSingleton();
        bind(DIEMBasicAuthProvider.class).asEagerSingleton();
        bind(S3Plugin.class).asEagerSingleton();
        bind(HibernateSearchPlugin.class).asEagerSingleton();
        bind(DAOProvider.class).asEagerSingleton();
        bind(FundraisingLinkDAO.class).asEagerSingleton();
        bind(UserProvider.class).asEagerSingleton();
        bind(StripeConnectService.class).asEagerSingleton();
        bind(PlaidLinkService.class).asEagerSingleton();
        bind(FeeUtility.class).asEagerSingleton();
        bind(OutgoingEmailService.class).asEagerSingleton();
        bind(PaymentTransactionFacade.class).asEagerSingleton();
        bind(PDFTemplateStamper.class).asEagerSingleton();
        bind(UserActivationService.class).asEagerSingleton();
        bind(FundraisingService.class).asEagerSingleton();
        bind(LinkPreviewService.class).asEagerSingleton();
        bind(SeoService.class).asEagerSingleton();
        bind(QuestMemberService.class).asEagerSingleton();
        bind(QuestService.class).asEagerSingleton();
        bind(TaskGroupService.class).asEagerSingleton();
        bind(ReportingService.class).asEagerSingleton();
        bind(LeaderboardService.class).asEagerSingleton();
        bind(NotificationService.class).asEagerSingleton();
        bind(FormSecurityService.class).asEagerSingleton();
        bind(ContentReportingService.class).asEagerSingleton();
        bind(CommentsService.class).asEagerSingleton();
        bind(CacheFixInstance.class).asEagerSingleton();
        bind(UserPaymentFeeService.class).asEagerSingleton();
        bind(RedisAuthProvider.class).asEagerSingleton();
    }

}

@Singleton
class CacheFixInstance {
    @Inject
    public CacheFixInstance(ApplicationLifecycle lifecycle) {
        lifecycle.addStopHook(() -> {
            CacheManager.getInstance().shutdown();

            Logger.info("EHCache manager has been shut down");

            return CompletableFuture.completedFuture(null);
        });
    }
}
