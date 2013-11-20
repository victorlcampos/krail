package uk.co.q3c.v7.base.guice.services;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.q3c.v7.base.guice.services.Service.Status;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class ServicesManagerModule extends AbstractModule {

	private static final Logger log = LoggerFactory.getLogger(ServicesManagerModule.class);

	public class ServicesListener implements TypeListener {
		private final ServicesManager servicesManager;

		public ServicesListener(ServicesManager servicesManager) {
			this.servicesManager = servicesManager;
		}

		@Override
		public <I> void hear(final TypeLiteral<I> type, TypeEncounter<I> encounter) {
			encounter.register(new InjectionListener<Object>() {
				@Override
				public void afterInjection(Object injectee) {

					// cast is safe - if not the matcher is wrong
					Service service = (Service) injectee;
					servicesManager.registerService(service);
					log.debug("auto-registered service '{}'", service.getName());
				}
			});
		}

	}

	private class ServiceMethodStartInterceptor implements MethodInterceptor {

		public ServiceMethodStartInterceptor() {
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			Service service = (Service) invocation.getThis();
			log.debug("starting service '{}'", service.getName());
			Status status = service.getStatus();
			switch (status) {
			case INITIAL:
			case STOPPED:
				Object result = null;
				try {
					result = invocation.proceed();
					return result;
				} catch (Throwable e) {
					throw e;
				}
			default:
				log.debug("The service {} is already started, start request ignored", invocation.getThis());
				return null;
			}
		}
	}

	private class ServiceMethodStopInterceptor implements MethodInterceptor {

		public ServiceMethodStopInterceptor() {
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			Service service = (Service) invocation.getThis();
			log.debug("stopping service '{}'", service.getName());
			Status status = service.getStatus();
			switch (status) {
			case STARTED:
				Object result = null;
				try {
					result = invocation.proceed();
					return result;
				} catch (Throwable e) {
					throw e;
				}
			default:
				log.debug("The service {} is already stopped, stop request ignored", invocation.getThis());
				return null;
			}
		}
	}

	private class FinalizeMethodMatcher extends AbstractMatcher<Method> {
		@Override
		public boolean matches(Method method) {
			return method.getName().equals("finalize");
		}
	}

	/**
	 * Calls {@link Service#stop} before passing on the finalize() call
	 * 
	 */
	private class FinalizeMethodInterceptor implements MethodInterceptor {

		public FinalizeMethodInterceptor() {
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			Service service = (Service) invocation.getThis();
			service.stop();
			return invocation.proceed();
		}
	}

	/**
	 * Matches classes implementing {@link Service}
	 * 
	 */
	private class ServiceInterfaceMatcher extends AbstractMatcher<TypeLiteral<?>> {
		@Override
		public boolean matches(TypeLiteral<?> t) {
			Class<?> rawType = t.getRawType();
			Class<?>[] interfaces = rawType.getInterfaces();
			for (Class<?> intf : interfaces) {
				if (intf.equals(Service.class)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Matches the {@link Service#start()} method
	 * 
	 */
	private class InterfaceStartMethodMatcher extends AbstractMatcher<Method> {
		@Override
		public boolean matches(Method method) {
			return method.getName().equals("start");
		}
	}

	/**
	 * Matches the {@link Service#stop()} method
	 * 
	 */
	private class InterfaceStopMethodMatcher extends AbstractMatcher<Method> {
		@Override
		public boolean matches(Method method) {
			return method.getName().equals("stop");
		}
	}

	/**
	 * Needs to be created this way because it is inside the module, but note that the @Provides method at
	 * getServicesManager() ensures that injection scope remains consistent
	 */
	private final ServicesManager servicesManager = new ServicesManager();

	@Override
	protected void configure() {

		bindListener(new ServiceInterfaceMatcher(), new ServicesListener(servicesManager));

		bindInterceptor(Matchers.subclassesOf(Service.class), new InterfaceStartMethodMatcher(),
				new ServiceMethodStartInterceptor());

		bindInterceptor(Matchers.subclassesOf(Service.class), new InterfaceStopMethodMatcher(),
				new ServiceMethodStopInterceptor());

		bindInterceptor(Matchers.subclassesOf(Service.class), new FinalizeMethodMatcher(),
				new FinalizeMethodInterceptor());

	}

	@Provides
	public ServicesManager getServicesManager() {
		return servicesManager;
	}

}