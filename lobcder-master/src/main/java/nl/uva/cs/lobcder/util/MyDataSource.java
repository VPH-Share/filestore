package nl.uva.cs.lobcder.util;

import lombok.Getter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * User: dvasunin
 * Date: 18.03.13
 * Time: 21:52
 * To change this template use File | Settings | File Templates.
 */
public class MyDataSource {

    @Getter
    private DataSource datasource;

    public MyDataSource() throws NamingException {
        String jndiName = "jdbc/lobcder";
        Context ctx = new InitialContext();
        Context envContext = (Context) ctx.lookup("java:/comp/env");
        datasource = (DataSource) envContext.lookup(jndiName);
    }


    public Connection getConnection() throws SQLException {
        Connection cn = datasource.getConnection();
        cn.setAutoCommit(false);
        return cn;
    }
}
