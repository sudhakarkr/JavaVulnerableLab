/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cysecurity.cspf.jvl.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author breakthesec
 */
public class AddPage extends HttpServlet {

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
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
           String fileName=request.getParameter("filename");
           String content=request.getParameter("content");
           if(fileName!=null && content!=null)
           {
            // Validate filename: allow only simple names (letters, digits, underscore, dash) with a safe extension.
            // Reject path separators, traversal sequences, and disallowed/executable extensions to prevent
            // arbitrary file write / web shell upload (CWE-434).
            if(!fileName.matches("[A-Za-z0-9_-]{1,64}\\.(txt|html|htm)"))
            {
                out.print("Invalid filename");
                return;
            }
            String pagesDir=getServletContext().getRealPath("/pages");
            File pagesDirFile=new File(pagesDir).getCanonicalFile();
            String filePath=pagesDir+"/"+fileName;
            File f=new File(filePath).getCanonicalFile();
            // Ensure the resolved file is still within the intended pages directory.
            if(f.getParentFile()==null || !f.getParentFile().equals(pagesDirFile))
            {
                out.print("Invalid filename");
                return;
            }
            if(f.exists())
            {
                f.delete();
            }
                if(f.createNewFile())
                {
                    BufferedWriter bw=new BufferedWriter(new FileWriter(f.getAbsoluteFile()));
                    bw.write(content);
                    bw.close();
                    out.print("Successfully created the file: <a href='../pages/"+fileName+"'>"+fileName+"</a>");
                }
                else
                {
                    out.print("Failed to create the file");
                }
           }
           else
           {
               out.print("filename or content Parameter is missing");
           }           
           
        } 
        catch(Exception e)
        {
            out.print(e);
        }
        finally {
            out.close();
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
