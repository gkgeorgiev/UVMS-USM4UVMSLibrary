package eu.europa.ec.fisheries.uvms.rest.security.resources;


import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import eu.europa.ec.fisheries.uvms.rest.security.bean.USMService;
import eu.europa.ec.fisheries.wsdl.user.types.Application;
import eu.europa.ec.fisheries.wsdl.user.types.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Path("/rest")
public class RestResource {

    final static Logger LOG = LoggerFactory.getLogger(RestResource.class);
    private static final String TEST_USER_PREFERENCE = "TEST_USER_PREFERENCE";
    public static final String SOME_TEST_OPTION = "someTestOption";
    public static final String DATASET_CATEGORY = "restrictionAreas";

    @Context
	private UriInfo context;

    @EJB
    private USMService usmService;
	


    @GET
	@Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
    public Response listReports(@Context HttpServletRequest request,
    				@Context HttpServletResponse response) {

        if (!request.isUserInRole("APP1_TEST_FEATURE")) {
            return Response.status(HttpServletResponse.SC_FORBIDDEN).build();
        }

    	return Response.status(HttpServletResponse.SC_OK).build();
    }

    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReport(@Context HttpServletRequest request,
                                @Context HttpServletResponse response) {

        if (!request.isUserInRole("NON_EXISTING_TEST_FEATURE")) {
            return Response.status(HttpServletResponse.SC_FORBIDDEN).build();
        }

        return Response.status(HttpServletResponse.SC_OK).build();
    }


    @GET
    @Path("/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    public Response datasets(@Context HttpServletRequest request,
                              @Context HttpServletResponse response)  {

        String datasetName = "datasetName" + new Date().getTime();
        String datasetDiscriminator = "Discriminator" + new Date().getTime();
        String cacheKey = "adgfasregfdsvdsfgrewtrehjyhsgaswefa";
        String datasetCategory = DATASET_CATEGORY + new Date().getTime();



        Dataset dataset = new Dataset();
        dataset.setCategory(datasetCategory);
        dataset.setName(datasetName);
        dataset.setDiscriminator(datasetName);

        /// let's test the setter method
        try {
            usmService.setDataset("Reporting", dataset, cacheKey);
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }


        eu.europa.ec.fisheries.wsdl.user.types.Context ctxt = null;
        try {
            ctxt = usmService.getUserContext("rep_power", "Reporting", "rep_power_role", "EC", "someCacheKey");
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        try {
            long timeDiff = new Date().getTime();
            List<Dataset> datasets = usmService.getDatasetsPerCategory(datasetCategory, ctxt);

            if (datasets == null
                    || datasets.size() != 1
                    || !datasetDiscriminator.equals(datasets.get(0).getDiscriminator())
                    || !datasetName.equals(datasets.get(0).getDiscriminator())
                    || !datasetCategory.equals(datasets.get(0).getCategory())) {
                return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
            }


            //now let's check the caching performance
            timeDiff = new Date().getTime() - timeDiff;

            long timeDiff2 = new Date().getTime();
            datasets = usmService.getDatasetsPerCategory(datasetCategory, ctxt);
            timeDiff2 = new Date().getTime() - timeDiff2;

            if (datasets == null || datasets.size() != 1 || timeDiff2>= timeDiff) {
                return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
            }

        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(HttpServletResponse.SC_OK).build();

    }

    @GET
    @Path("/applicationDescriptor")
    @Produces(MediaType.APPLICATION_JSON)
    public Response applicationDescriptor(@Context HttpServletRequest request,
                              @Context HttpServletResponse response) {
        long timeDiff = new Date().getTime();

        Application application = null;
        try {
            application = usmService.getApplicationDefinition("Reporting");
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        timeDiff = new Date().getTime() - timeDiff;


        if (application== null
                || !"Reporting".equals(application.getName())) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(String.valueOf(timeDiff)).build();
        }

        long timeDiff2 = new Date().getTime();
        try {
            application = usmService.getApplicationDefinition("Reporting");
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }
        timeDiff2 = new Date().getTime()-timeDiff2;

        if (application== null
                || !"Reporting".equals(application.getName())
                || timeDiff2>= timeDiff) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).entity(String.valueOf(timeDiff)).build();
        }

        return Response.status(HttpServletResponse.SC_OK).entity(String.valueOf(timeDiff)).build();
    }

    @GET
    @Path("/options")
    @Produces(MediaType.APPLICATION_JSON)
    public Response options(@Context HttpServletRequest request,
                              @Context HttpServletResponse response) {

        String newDefaultValue = "newValue"+ new Date().getTime();

        try {
            usmService.setOptionDefaultValue(TEST_USER_PREFERENCE, newDefaultValue, "DEPLOY_APPLICATION");
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }


        String optionDefaultValue;
        long timeDiff = new Date().getTime();
        try {
            optionDefaultValue = usmService.getOptionDefaultValue(TEST_USER_PREFERENCE, "DEPLOY_APPLICATION");
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }
        timeDiff = new Date().getTime() - timeDiff;

        if (optionDefaultValue == null
                || !newDefaultValue.equals(optionDefaultValue)) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        timeDiff = new Date().getTime() - timeDiff;

        long timeDiff2 = new Date().getTime();
        try {
            optionDefaultValue = usmService.getOptionDefaultValue(TEST_USER_PREFERENCE, "DEPLOY_APPLICATION");
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }
        timeDiff2 = new Date().getTime() - timeDiff2;

        if (optionDefaultValue == null
                || !newDefaultValue.equals(optionDefaultValue)
                || timeDiff2 >= timeDiff) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(HttpServletResponse.SC_OK).build();
    }

    @GET
    @Path("/features")
    @Produces(MediaType.APPLICATION_JSON)
    public Response features(@Context HttpServletRequest request,
                              @Context HttpServletResponse response)  {
        eu.europa.ec.fisheries.wsdl.user.types.Context ctxt = null;
        try {
            ctxt = usmService.getUserContext("rep_power", "Reporting", "rep_power_role", "EC", "someCacheKey");
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        Set<String> features;

        try {
            features = usmService.getUserFeatures("rep_power", ctxt);
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        if (features == null
                || features.size()==0) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        try {
            features = usmService.getUserFeatures("rep_power", ctxt);
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        if (features == null
                || features.size()==0) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(HttpServletResponse.SC_OK).build();
    }

    @GET
    @Path("/preferences")
    @Produces(MediaType.APPLICATION_JSON)
    public Response preferences(@Context HttpServletRequest request,
                              @Context HttpServletResponse response) {

        String newValue = "valueeeee" + new Date().getTime();

        try {
            usmService.updateUserPreference(TEST_USER_PREFERENCE, newValue,  "TEST_APP", "EC", "rep_power", "someCacheKeyrefga wqerf qwerdfqwre23");
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        eu.europa.ec.fisheries.wsdl.user.types.Context ctxt = null;
        try {
            ctxt = usmService.getUserContext("rep_power", "Reporting", "rep_power_role", "EC", "someCacheKeydratgv asdf aert34sdfgds");
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        String userPref = null;
        try {
            userPref = usmService.getUserPreference(SOME_TEST_OPTION, ctxt);
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        if (!newValue.equals(userPref)) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }


        try {
            userPref = usmService.getUserPreference(SOME_TEST_OPTION, ctxt);
        } catch (ServiceException e) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        if (!newValue.equals(userPref)) {
            return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(HttpServletResponse.SC_OK).build();
    }
}
