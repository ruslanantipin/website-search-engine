package searchengine.repositories;

import searchengine.dto.indexing.DataResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBConnection {
    private static Connection connection;

    private static String dbName = "search_engine";
    private static String dbUser = "root";
    private static String dbPass = "123";

    //private static StringBuilder insertQuery = new StringBuilder();
    //private static StringBuilder insertQueryIndex = new StringBuilder();

    private static Connection getConnection()
    {
        if(connection == null){
            try {
                connection = DriverManager.getConnection(
                        "jdbc:mysql://localhost:3306/" + dbName +
                                "?user=" + dbUser + "&password=" + dbPass);

            } catch (SQLException e){
                e.printStackTrace();
            }
        }
        return connection;
    }

//    public static void countWords(String word, int siteId, int pageId)
//    {
//        insertQuery.append((insertQuery.length() == 0 ? "" : " UNION ") +
//                "(SELECT '" + siteId + "', '" + word + "', 1)");
//        insertQueryIndex.append((insertQueryIndex.length() == 0 ? "" : " UNION ALL ") +
//                        " (SELECT id, '" + pageId + "', '1' FROM lemma WHERE lemma = '" + word + "')");
//        /*insertQueryIndex.append("INSERT INTO index_table(lemma_id, page_id, rank_column)" +
//                " (select id, '" + pageId + "', '1' from lemma where lemma = '" + word + "')"
//                + " ON DUPLICATE KEY UPDATE `rank_column`=`rank_column` + 1;");*/
//
//    }

    public synchronized static void executeMultiInsert(StringBuilder insertQuery, StringBuilder insertQueryIndex) throws SQLException
    {
        try {
            String sql = "INSERT INTO lemma(site_id, lemma, frequency) " +
                    insertQuery.toString() +
                    "ON DUPLICATE KEY UPDATE `frequency`= `frequency` + 1;";
            DBConnection.getConnection().createStatement().execute(sql);
        }catch(Exception e){
            e.printStackTrace();
        }
        //insertQuery = new StringBuilder();

        String sqlIndex = "INSERT INTO index_table(lemma_id, page_id, rank_column)" +
                 insertQueryIndex.toString() +
                 " ON DUPLICATE KEY UPDATE `rank_column`=`rank_column` + 1;";
        //String sqlIndex = insertQueryIndex.toString();
        DBConnection.getConnection().createStatement().execute(sqlIndex);
        //insertQueryIndex = new StringBuilder();
    }

    public static ArrayList<DataResponse> getIdPages(String query) throws SQLException
    {
        ArrayList<DataResponse> idPages = new ArrayList<>();
        ResultSet res = DBConnection.getConnection().createStatement().executeQuery(query);
        int numColumns = res.getMetaData().getColumnCount();
        while (res.next())
        {
            if (numColumns == 5){
                DataResponse data = new DataResponse();
                data.setRelevance((Double) res.getObject(2));
                data.setSnippet((String) res.getObject(3));
                data.setSite((String) res.getObject(4));
                data.setSiteName((String) res.getObject(5));
                idPages.add(data);
            }
        }
        return idPages;
    }


}
