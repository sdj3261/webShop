package ex01;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet implementation class BoardController
 */
@WebServlet("/board/*")
public class BoardController extends HttpServlet {
    private static final String ARTICLE_IMAGE_REPOSITORY = "C:\\board\\article_image";
    BoardService boardService;
    ArticleVO articleVO;

    /**
     * @see Servlet#init(ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        boardService = new BoardService();
        articleVO = new ArticleVO();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)	throws ServletException, IOException {
        doHandle(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)	throws ServletException, IOException {
        doHandle(request, response);
    }

    private void doHandle(HttpServletRequest request, HttpServletResponse response)	throws ServletException, IOException {
        String nextPage = "";
        request.setCharacterEncoding("utf-8");
        response.setContentType("text/html; charset=utf-8");
        HttpSession session;
        String action = request.getPathInfo();
        System.out.println("action:" + action);
        try {
            List<ArticleVO> articlesList = new ArrayList<ArticleVO>();
            if (action ==   null) {
                articlesList = boardService.listArticles();
                request.setAttribute("articlesList", articlesList);
                nextPage = "/ex01/listArticles.jsp";
            } else if (action.equals("/listArticles.do")) {
                articlesList = boardService.listArticles();
                request.setAttribute("articlesList", articlesList);
                nextPage = "/ex01/listArticles.jsp";
            } else if (action.equals("/articleForm.do")) {
                nextPage = "/ex01/articleForm.jsp";
            } else if (action.equals("/addArticle.do")) {
                int articleNo = 0;
                Map<String,String> articleMap = upload(request,response);
                String content = articleMap.get("content");
                String imageFileName = articleMap.get("imageFileName");
                String title = articleMap.get("title");
                System.out.println("title =========== : " + title);
                articleVO.setParentNO(0);
                articleVO.setId("hong");
                articleVO.setTitle(title);
                articleVO.setContent(content);
                articleVO.setImageFileName(imageFileName);

                articleNo = boardService.addArticle(articleVO);

                if(imageFileName != null && imageFileName.length() !=0) {
                    File srcFile = new File (ARTICLE_IMAGE_REPOSITORY + "\\" + "temp" + "\\" + imageFileName);
                    File destDir = new File(ARTICLE_IMAGE_REPOSITORY + "\\" + articleNo);
                    destDir.mkdirs();
                    FileUtils.moveFileToDirectory(srcFile, destDir, true);
                }
                PrintWriter pw = response.getWriter();;
                pw.print("<script>" + " alert('새글을 추가했습니다!');" + " location.href= '" + request.getContextPath() + "/board/listArticles.do';" + "</script>");
                return;
            }
            else if(action.equals("/viewArticle.do")) {
                String articleNo = request.getParameter("articleNO");
                articleVO = boardService.viewArticle(Integer.parseInt(articleNo));
                request.setAttribute("article", articleVO);
                nextPage = "/ex01/viewArticle.jsp";
            }
            else if(action.equals("/modArticle.do")) {
                Map<String,String> articleMap = upload(request,response);
                int articleNo = Integer.parseInt(articleMap.get("articleNO"));
                articleVO.setArticleNO(articleNo);
                String title = articleMap.get("title");
                String content = articleMap.get("content");
                String imageFileName = articleMap.get("imageFileName");
                articleVO.setParentNO(0);
                articleVO.setId("hong");
                articleVO.setTitle(title);
                articleVO.setContent(content);
                if(imageFileName != null) {
                    articleVO.setImageFileName(imageFileName);
                }
                boardService.modArticle(articleVO);

                if(imageFileName != null && imageFileName.length() != 0) {
                    String originalFileName = articleMap.get("originalFileName");
                    File srcFile = new File(ARTICLE_IMAGE_REPOSITORY + "\\" + "temp" + "\\" +imageFileName);
                    File destDir = new File(ARTICLE_IMAGE_REPOSITORY + "\\" + articleNo);
                    destDir.mkdirs();
                    FileUtils.moveFileToDirectory(srcFile,destDir, true);
                    File oldFile = new File(ARTICLE_IMAGE_REPOSITORY + "\\" + articleNo + "\\" + originalFileName);
                    if(oldFile.delete()) {
                        System.out.println("기존 파일 삭제 : " + oldFile.getName());
                    }
                }
                PrintWriter pw = response.getWriter();
                pw.print("<script>" +
                        " alert('글을 수정했습니다!');" + " location.href= '" + request.getContextPath() + "/board/viewArticle.do?articleNo=" + articleNo + "';"
                        + "</script>");
            }
            else if(action.equals("/removeArticle.do")) {
                int articleNo = Integer.parseInt(request.getParameter("articleNO"));
                List<Integer> articleNoList = boardService.removeArticle(articleNo);
                for(int _articleNo : articleNoList) {
                    File imgDir = new File(ARTICLE_IMAGE_REPOSITORY + "\\" + _articleNo);
                    if(imgDir.exists()) {
                        FileUtils.deleteDirectory(imgDir);
                    }
                }

                PrintWriter pw = response.getWriter();
                pw.print("<script>" +
                        " alert('글을 삭제했습니다!');" + " location.href= '" + request.getContextPath() + "/board/listArticles.do';"
                        + "</script>");
            }
            else if(action.equals("/replyForm.do")) {
                int parentNO = Integer.parseInt(request.getParameter("parentNO"));
                session = request.getSession();
                session.setAttribute("parentNO", parentNO);
                nextPage = "/ex01/replyForm.jsp";
            }
            else if (action.equals("/addReply.do")) {
                session = request.getSession();
                int parentNO = (Integer) session.getAttribute("parentNO");
                session.removeAttribute("parentNO");
                Map<String,String> articleMap = upload(request,response);
                String title = articleMap.get("title");
                String content = articleMap.get("content");
                String imageFileName = articleMap.get("imageFileName");

                articleVO.setParentNO(parentNO);
                articleVO.setId("lee");
                articleVO.setTitle(title);
                articleVO.setContent(content);
                articleVO.setImageFileName(imageFileName);


            }
            else {
                nextPage = "/ex01/listArticles.jsp";
            }

            RequestDispatcher dispatch = request.getRequestDispatcher(nextPage);
            dispatch.forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //mybatis 연동
    private Map<String, String> upload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String,String> articleMap = new HashMap<String, String>();
        String encoding = "utf-8";
        File currentDirPath = new File(ARTICLE_IMAGE_REPOSITORY);
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setRepository(currentDirPath);
        factory.setSizeThreshold(1024 * 1024);
        ServletFileUpload upload = new ServletFileUpload(factory);
        try {
            List items = upload.parseRequest(request);
            for (int i=0; i<items.size();i++) {
                FileItem fileItem = (FileItem) items.get(i);
                if (fileItem.isFormField()) {
                    System.out.println(fileItem.getFieldName() + "=" +fileItem.getString(encoding));
                    articleMap.put(fileItem.getFieldName(), fileItem.getString(encoding));
                } else {
                    System.out.println("파라미터이름 : " + fileItem.getFieldName());
                    System.out.println("파일이름 : " + fileItem.getName());
                    System.out.println("파일크기 " + fileItem.getSize());

                    if(fileItem.getSize() > 0) {
                        int idx = fileItem.getName().lastIndexOf("\\");
                        if (idx == -1) {
                            idx = fileItem.getName().lastIndexOf("/");
                        }

                        String fileName = fileItem.getName().substring(idx+1);
                        articleMap.put(fileItem.getFieldName(), fileName);
                        System.out.println("검증" + articleMap.get("imageFileName"));
                        File uploadFile = new File(currentDirPath + "\\temp\\" + fileName);
                        fileItem.write(uploadFile);
                    }
                }
            }
        } catch (FileUploadException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return articleMap;
    }
}
