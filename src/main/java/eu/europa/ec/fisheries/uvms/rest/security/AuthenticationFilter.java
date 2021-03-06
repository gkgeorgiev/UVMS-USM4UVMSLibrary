/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.rest.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.europa.ec.fisheries.uvms.constants.AuthConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import eu.cec.digit.ecas.client.j2ee.weblogic.EcasUser;
//import weblogic.security.spi.WLSUser;

/**
 * Filters incoming requests, converting JWT token to a remote 
 * user identity (if the request does not already reference a remote 
 * user), extending the duration of the JWT token (if present).
 *
 */
public class AuthenticationFilter implements Filter {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);
  private static final String CHALLENGEAUTH = "/challengeauth";
  private static final String AUTHENTICATE = "/authenticate";
  private static final String PING = "/ping";

  /**
   * Creates a new instance
   */
  public AuthenticationFilter() {
  }

  /**
   * Filters an incoming request, converting a (custom) JWT token to 
   * a (standard) remote user identity (if the request does not 
   * already reference a remote user), extending the duration of 
   * the JWT token (if present). If the request contains neither a remote
   * user identity nor a JWT token, request processing is skipped and an HTTP
   * status of 403 (Forbidden) is sent back to the requester,
   *
   * @param request The request we are processing
   * @param response The response we are creating
   * @param chain The filter chain we are processing
   *
   * @exception IOException if an input/output error occurs
   * @exception ServletException if another error occurs
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain)
  throws IOException, ServletException 
  {


    JwtTokenHandler tokenHandler = new JwtTokenHandler();

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    LOGGER.info("doFilter(" + httpRequest.getMethod() + ", " + 
                httpRequest.getPathInfo() + ") - (ENTER)");

    Boolean tokenIsUsed = false;
    String remoteUser = httpRequest.getRemoteUser();
    String jwtToken = httpRequest.getHeader(AuthConstants.HTTP_HEADER_AUTHORIZATION);

    LOGGER.info("httpRequest.getRemoteUser(): " + remoteUser);
    if (remoteUser == null) {
      tokenIsUsed = true;
      // decode token
      remoteUser = tokenHandler.parseToken(jwtToken);
    }
    LOGGER.info("remoteUser: " + remoteUser);

    // check whether is an authenticated request or not
    if (remoteUser != null) {
      UserRoleRequestWrapper arequest = new UserRoleRequestWrapper(httpRequest,
                                                               remoteUser);
      String refreshedToken;
      if (tokenIsUsed) {
        refreshedToken = tokenHandler.extendToken(jwtToken);
      } else {
        // we have a remote user but no token was provided
        refreshedToken = tokenHandler.createToken(remoteUser);
      }
      httpResponse.addHeader(AuthConstants.HTTP_HEADER_AUTHORIZATION, refreshedToken);
     
      if(PING.equals(httpRequest.getPathInfo())){
        //if(httpRequest.getUserPrincipal().getClass().equals(eu.cec.digit.ecas.client.j2ee.weblogic.EcasUser.class)) {
			if(httpRequest.getUserPrincipal() != null && httpRequest.getUserPrincipal().getClass().toString().contains("cas")) {
          LOGGER.info("ECAS Authenticated");
          //EcasUser ecasUser = (EcasUser) httpRequest.getUserPrincipal();
          //LOGGER.info("getEmail "+ecasUser.getEmail());
          //LOGGER.info("getUid "+ecasUser.getUid());
          String callback = httpRequest.getParameter(AuthConstants.JWTCALLBACK);
          if(callback!=null){
            LOGGER.info("Redirecting to add jwt");
            String redir = callback+"?jwt="+refreshedToken;
            httpResponse.sendRedirect(redir); 
          }
        }
      } 
      chain.doFilter(arequest, httpResponse);
    } else {
      String pathInfo = httpRequest.getPathInfo();

      if (AUTHENTICATE.equals(pathInfo) || CHALLENGEAUTH.equals(pathInfo)) {
        // if there is an authentication request proceed
        chain.doFilter(httpRequest, response);
      } else {
        //  Send 403 error
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
      }
    }

    LOGGER.info("doFilter() - (LEAVE)");
  }

  @Override
  public void init(FilterConfig fc)
  throws ServletException 
  {
    /*if (tokenHandler == null) {
      throw new ServletException("JwtTokenHandler is undefined");
    }*/
  }

  @Override
  public void destroy() {
    // NOP
  }

}