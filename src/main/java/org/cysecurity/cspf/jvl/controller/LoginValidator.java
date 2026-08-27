/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cysecurity.cspf.jvl.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.cysecurity.cspf.jvl.model.DBConnect;
 
 

/**
 *
 * @author breakthesec
 */
public class LoginValidator extends HttpServlet {

    /**
     * Hashes a plaintext password using SHA-256 for comparison against
     * the stored password hash. This avoids handling or comparing passwords
     * in plaintext at the persistence layer.
     */
    private static String hashPassword(String plaintext) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(plaintext.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
       
       String user=request.getParameter("username").trim();
          String pass=request.getParameter("password").trim();
           try
             {
                 Connection con=new DBConnect().connect(getServletContext().getRealPath("/WEB-INF/config.properties"));
                    if(con!=null && !con.isClosed())
                               {
                                   ResultSet rs=null;
                                   String hashedPass = hashPassword(pass);
                                   PreparedStatement ps = con.prepareStatement("select * from users where username=? and password=?");
                                   ps.setString(1, user);
                                   ps.setString(2, hashedPass);
                                   rs = ps.executeQuery();
                                   if(rs != null && rs.next()){
                                   HttpSession session=request.getSession();
                                   session.setAttribute("isLoggedIn", "1");
                                   session.setAttribute("userid", rs.getString("id"));
                                   session.setAttribute("user", rs.getString("username"));
                                   session.setAttribute("avatar", rs.getString("avatar"));
                                   Cookie privilege=new Cookie("privilege","user");
                                   response.addCookie(privilege);
                                   if(request.getParameter("RememberMe")!=null)
                                   {
                                       Cookie username=new Cookie("username",user);
                                       Cookie password=new Cookie("password",pass);
                                       response.addCookie(username);
                                        response.addCookie(password);
                                   }
                                   response.sendRedirect(response.encodeURL("ForwardMe?location=/index.jsp"));
                                   }
                                   else
                                   {
                                          response.sendRedirect("ForwardMe?location=/login.jsp&err=Invalid Username or Password");
                                   }
                                    
                               }
                }
               catch(Exception ex)
                {
                           response.sendRedirect("login.jsp?err=something went wrong");
                 }
        
    }
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
