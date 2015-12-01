package eu.europa.ec.fisheries.uvms.init;

import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import eu.europa.ec.fisheries.uvms.jms.USMMessageConsumer;
import eu.europa.ec.fisheries.uvms.jms.USMMessageProducer;
import eu.europa.ec.fisheries.uvms.message.AbstractJAXBMarshaller;
import eu.europa.ec.fisheries.uvms.message.MessageConsumer;
import eu.europa.ec.fisheries.uvms.message.MessageException;
import eu.europa.ec.fisheries.uvms.rest.security.bean.USMService;
import eu.europa.ec.fisheries.uvms.user.model.exception.ModelMarshallException;
import eu.europa.ec.fisheries.uvms.user.model.mapper.UserModuleRequestMapper;
import eu.europa.ec.fisheries.wsdl.user.module.*;
import eu.europa.ec.fisheries.wsdl.user.types.Application;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.jms.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

public abstract class AbstractModuleInitializerBean extends AbstractJAXBMarshaller {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractModuleInitializerBean.class);

    private static final Long UVMS_USM_TIMEOUT = 10000L;

    @EJB
    protected USMService usmService;

    @PostConstruct
    public void onStartup() throws IOException, JAXBException, ModelMarshallException, JMSException, ServiceException, MessageException {
        // do something on application startup
        InputStream deploymentDescInStream = getDeploymentDescriptorRequest();

        if (deploymentDescInStream != null) {
            String deploymentDescriptor = IOUtils.toString(deploymentDescInStream, "UTF-8");
            if (!isAppDeployed(deploymentDescriptor)) {
                usmService.deployApplicationDescriptor(deploymentDescriptor);
            }
        } else {
            LOG.error("USM deployment descriptor is not provided, therefore, the JMS deployment message cannot be sent.");
        }
    }

    private boolean isAppDeployed(String deploymentDescriptor) throws JAXBException, JMSException, ServiceException, MessageException {
        boolean isAppDeployed = false;
        JAXBContext jaxBcontext = JAXBContext.newInstance(Application.class);
        javax.xml.bind.Unmarshaller um = jaxBcontext.createUnmarshaller();

        Application applicationDefinition = (Application) um.unmarshal(new StringReader(deploymentDescriptor));

        Application application = usmService.getApplicationDefinition(applicationDefinition.getName());

        if (application != null) {
            isAppDeployed = true;
        }

        return isAppDeployed;
    }

    @PreDestroy
    public void onShutdown() {
        //TODO undeploy app
    }

    protected abstract InputStream getDeploymentDescriptorRequest();
}
