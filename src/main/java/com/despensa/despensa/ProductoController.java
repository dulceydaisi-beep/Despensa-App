package com.despensa.despensa;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.*;
import java.util.*;


@RestController
@RequestMapping("/productos")
public class ProductoController {

    String url = "jdbc:postgresql://dpg-d7k43e4p3tds73baj47g-a.oregon-postgres.render.com:5432/despensa_db";
    String user = "despensa_db_user";
    String pass = "jXzNHi4XSWcetOe83xaB7HHdMTbL2V6T";

    @GetMapping
    public List<Map<String, Object>> productos() throws Exception {

        Connection con = DriverManager.getConnection(url, user, pass);
        Statement crear = con.createStatement();

        crear.execute("""
CREATE TABLE IF NOT EXISTS productos (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(255),
    categoria VARCHAR(255),
    precio DOUBLE PRECISION,
    stock_actual INT,
    stock_minimo INT,
    proveedor VARCHAR(255)
)
""");
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM productos");

        List<Map<String, Object>> lista = new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> p = new HashMap<>();
            p.put("id", rs.getInt("id"));
            p.put("nombre", rs.getString("nombre"));
            p.put("precio", rs.getDouble("precio"));
            p.put("stock", rs.getInt("stock_actual"));
            p.put("categoria", rs.getString("categoria"));
            lista.add(p);
        }

        con.close();
        return lista;

    }
    @GetMapping("/resumen")
    public Map<String, Integer> resumen() throws Exception {

        Connection con = DriverManager.getConnection(url, user, pass);

        Map<String, Integer> datos = new HashMap<>();

        // Total productos
        Statement st1 = con.createStatement();
        ResultSet rs1 = st1.executeQuery("SELECT COUNT(*) FROM productos");
        rs1.next();
        datos.put("totalProductos", rs1.getInt(1));

        // Stock total
        Statement st2 = con.createStatement();
        ResultSet rs2 = st2.executeQuery("SELECT SUM(stock_actual) FROM productos");
        rs2.next();
        datos.put("stockTotal", rs2.getInt(1));

        // Productos con stock bajo (<5)
        Statement st3 = con.createStatement();
        ResultSet rs3 = st3.executeQuery("SELECT COUNT(*) FROM productos WHERE stock_actual < 5");
        rs3.next();
        datos.put("stockBajo", rs3.getInt(1));

        con.close();

        return datos;
    }
       @PostMapping("/vender/{id}")
    public String vender(@PathVariable int id) throws Exception {

        Connection con = DriverManager.getConnection(url, user, pass);

        // 1. buscar stock actual
        PreparedStatement ps = con.prepareStatement(
                "SELECT stock_actual FROM productos WHERE id = ?"
        );
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int stock = rs.getInt("stock_actual");

            if (stock > 0) {
                // 2. descontar stock
                PreparedStatement ps2 = con.prepareStatement(
                        "UPDATE productos SET stock_actual = stock_actual - 1 WHERE id = ?"
                );
                ps2.setInt(1, id);
                ps2.executeUpdate();
            }
        }

        con.close();
        return "OK";
    }
    @PostMapping("/agregar/{id}")
    public String agregarStock(@PathVariable int id) throws Exception {

        Connection con = DriverManager.getConnection(url, user, pass);

        PreparedStatement ps = con.prepareStatement(
                "UPDATE productos SET stock_actual = stock_actual + 1 WHERE id = ?"
        );
        ps.setInt(1, id);
        ps.executeUpdate();

        con.close();
        return "OK";
    }
    @PostMapping("/precio/{id}")
    public String cambiarPrecio(@PathVariable int id, @RequestParam double precio) throws Exception {

        Connection con = DriverManager.getConnection(url, user, pass);

        PreparedStatement ps = con.prepareStatement(
                "UPDATE productos SET precio = ? WHERE id = ?"
        );
        ps.setDouble(1, precio);
        ps.setInt(2, id);
        ps.executeUpdate();

        con.close();
        return "OK";
    }
    @GetMapping("/nuevo")
    public String nuevoProducto(
            @RequestParam String nombre,
            @RequestParam double precio,
            @RequestParam int stock,
            @RequestParam String categoria
    ) throws Exception {

        Connection con = DriverManager.getConnection(url, user, pass);

        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO productos(nombre, categoria, precio, stock_actual, stock_minimo, proveedor) VALUES (?, ?, ?, ?, ?, ?)"
        );

        ps.setString(1, nombre);
        ps.setString(2, categoria);
        ps.setDouble(3, precio);
        ps.setInt(4, stock);
        ps.setInt(5, 0); // stock mínimo default
        ps.setString(6, ""); // proveedor vacío

        ps.executeUpdate();

        con.close();


        return "OK";


    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable int id) throws Exception {

        Connection con = DriverManager.getConnection(url, user, pass);

        PreparedStatement ps = con.prepareStatement(
                "DELETE FROM productos WHERE id = ?"
        );

        ps.setInt(1, id);

        ps.executeUpdate();

        con.close();

        return "Producto eliminado";
    }
}

