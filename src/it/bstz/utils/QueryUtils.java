package it.bstz.utils;

//this is needed if you want to use capitalize() instead of the legacy way
//import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class QueryUtils {

    /**
     * Returns a list with the fields listed in the given sql query string
     * @param query the query to be analyzed
     * @return a List of strings with the fields names
     * @throws Exception When SELECT of FROM statements are not found in query
     */
    public static List<String> getQueryFields(String query) throws Exception {
        int selectStmntIndex = query.toLowerCase().indexOf("select");
        if (selectStmntIndex == -1) {
            throw new Exception("\"SELECT\" statement not found in query: \"" + query + "\"");
        }
        int fromStmntIndex = query.indexOf("from");
        if (fromStmntIndex == -1) {
            throw new Exception("\"FROM\" statement not found in query: \"" + query + "\"");
        }

        // +6 is the length of the SELECT word
        String queryFields = query.substring(selectStmntIndex + 6, fromStmntIndex).trim();

        // \\s* means that there could be zero or more spaces around each comma
        List<String> fields = Arrays.asList(queryFields.split("\\s*,\\s*"));
        fields.replaceAll(f -> {
            if (f.contains("as")) {
                f = f.split("\\s*as\\s*")[1];
            }
            // you need commons-lang for this
            //f = StringUtils.capitalize(f);

            //this approach remove the needs of a library
            f = f.substring(0, 1).toUpperCase() + f.substring(1);
            return f;
        });
        return fields;
    }

    /**
     * Add the schema to an existing sql query string
     * @param query The query to be modified
     * @param schema The schema you want to add to the query
     * @return a modified version of the original query with the schema before the table name
     * @throws Exception When FROM statement is not found in query
     */
    public static String addSchemaToQuery(String query, String schema) throws Exception {
        String tableName = getTableName(query);
        int tableNameIndex = query.indexOf(tableName);
        return query.substring(0, tableNameIndex) + schema + "." + query.substring(tableNameIndex);
    }

    /**
     * Get the table name from a given sql query string
     * @param query The query to extract the table name from
     * @return The table name without any alias
     * @throws Exception When FROM statement is not found in query
     */
    public static String getTableName(String query) throws Exception {
        int fromStmntIndex = query.toLowerCase().indexOf("from");
        if (fromStmntIndex == -1) {
            throw new Exception("\"FROM\" statement not found in query: \"" + query + "\"");
        }
        int whereStmntIndex = query.toLowerCase().indexOf("where");

        // substring must end before WHERE clause or at the end of the query if there's no WHERE statement
        int endOfTableNameSubstringPosition = whereStmntIndex < 0 ? query.length() : Math.min(whereStmntIndex, query.length());

        return query.substring(fromStmntIndex + 4, endOfTableNameSubstringPosition).trim().replace(";", "") // remove trailing semicolon, if any
                .split("\\s")[0]; // take the first part (excluding table alias)
    }
}
