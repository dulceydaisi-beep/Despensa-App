package com.despensa.despensa;

import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/productos")
public class ProductoController {

    private final String url = System.getenv("SPRING_DATASOURCE_URL");
    private final String user = System.getenv("SPRING_DATASOURCE_USERNAME");
    private final String pass = System.getenv("SPRING_DATASOURCE_PASSWORD");

    // Inicializa la estructura de la base de datos si no existe
    private void inicializarBaseDatos() {
        try (Connection con = DriverManager.getConnection(url, user, pass);
             Statement st = con.createStatement()) {

            st.execute("""
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

            st.execute("""
                CREATE TABLE IF NOT EXISTS ventas (
                    id SERIAL PRIMARY KEY,
                    producto_id INT,
                    nombre_producto VARCHAR(255),
                    precio DOUBLE PRECISION,
                    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping
    public List<Map<String, Object>> productos() throws Exception {
        inicializarBaseDatos();
        Connection con = DriverManager.getConnection(url, user, pass);
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM productos ORDER BY id DESC");

        List<Map<String, Object>> lista = new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> p = new HashMap<>();
            p.put("id", rs.getInt("id"));
            p.put("nombre", rs.getString("nombre"));
            p.put("precio", rs.getDouble("precio"));
            p.put("stock", rs.getInt("stock_actual"));
            p.put("stockMinimo", rs.getInt("stock_minimo"));
            p.put("categoria", rs.getString("categoria"));
            lista.add(p);
        }

        con.close();
        return lista;
    }


    @PostMapping("/nuevo")
    public String nuevoProducto(@RequestBody Map<String, Object> body) throws Exception {
        inicializarBaseDatos();

        String nombre = body.get("nombre").toString();
        String categoria = body.getOrDefault("categoria", "Gral").toString();
        double precio = Double.parseDouble(body.get("precio").toString());
        int stock = Integer.parseInt(body.get("stock").toString());
        int minimo = body.containsKey("minimo") && !body.get("minimo").toString().isEmpty()
                ? Integer.parseInt(body.get("minimo").toString()) : 2;

        try (Connection con = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO productos(nombre, categoria, precio, stock_actual, stock_minimo, proveedor) VALUES (?, ?, ?, ?, ?, ?)"
             )) {

            ps.setString(1, nombre);
            ps.setString(2, categoria);
            ps.setDouble(3, precio);
            ps.setInt(4, stock);
            ps.setInt(5, minimo);
            ps.setString(6, "General");

            ps.executeUpdate();
        }

        return "OK";
    }
    @PostMapping("/vender/{id}")
    public String vender(@PathVariable int id) throws Exception {
        Connection con = DriverManager.getConnection(url, user, pass);

        PreparedStatement psSel = con.prepareStatement("SELECT nombre, precio, stock_actual FROM productos WHERE id = ?");
        psSel.setInt(1, id);
        ResultSet rs = psSel.executeQuery();

        if (rs.next()) {
            int stockActual = rs.getInt("stock_actual");
            String nombre = rs.getString("nombre");
            double precio = rs.getDouble("precio");

            if (stockActual > 0) {
                PreparedStatement psUpd = con.prepareStatement("UPDATE productos SET stock_actual = stock_actual - 1 WHERE id = ?");
                psUpd.setInt(1, id);
                psUpd.executeUpdate();

                PreparedStatement psVenta = con.prepareStatement("INSERT INTO ventas(producto_id, nombre_producto, precio) VALUES (?, ?, ?)");
                psVenta.setInt(1, id);
                psVenta.setString(2, nombre);
                psVenta.setDouble(3, precio);
                psVenta.executeUpdate();
            }
        }

        con.close();
        return "OK";
    }

    @PostMapping("/agregar/{id}")
    public String agregarStock(@PathVariable int id) throws Exception {
        Connection con = DriverManager.getConnection(url, user, pass);
        PreparedStatement ps = con.prepareStatement("UPDATE productos SET stock_actual = stock_actual + 1 WHERE id = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
        con.close();
        return "OK";
    }

    @PostMapping("/precio/{id}")
    public String editarPrecio(@PathVariable int id, @RequestParam double precio) throws Exception {
        Connection con = DriverManager.getConnection(url, user, pass);
        PreparedStatement ps = con.prepareStatement("UPDATE productos SET precio = ? WHERE id = ?");
        ps.setDouble(1, precio);
        ps.setInt(2, id);
        ps.executeUpdate();
        con.close();
        return "OK";
    }

    @DeleteMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable int id) throws Exception {
        Connection con = DriverManager.getConnection(url, user, pass);
        PreparedStatement ps = con.prepareStatement("DELETE FROM productos WHERE id = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
        con.close();
        return "OK";
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() throws Exception {
        inicializarBaseDatos();
        Connection con = DriverManager.getConnection(url, user, pass);
        Statement st = con.createStatement();

        ResultSet rs1 = st.executeQuery("SELECT COUNT(*), COALESCE(SUM(stock_actual), 0) FROM productos");
        rs1.next();
        int totalProductos = rs1.getInt(1);
        int stockTotal = rs1.getInt(2);

        ResultSet rs2 = st.executeQuery("SELECT COUNT(*) FROM productos WHERE stock_actual <= stock_minimo");
        rs2.next();
        int stockBajo = rs2.getInt(1);

        Map<String, Object> res = new HashMap<>();
        res.put("totalProductos", totalProductos);
        res.put("stockTotal", stockTotal);
        res.put("stockBajo", stockBajo);

        con.close();
        return res;
    }

    @GetMapping("/resumenVentas")
    public Map<String, Object> resumenVentas() throws Exception {
        inicializarBaseDatos();
        Connection con = DriverManager.getConnection(url, user, pass);
        Statement st = con.createStatement();

        ResultSet rs = st.executeQuery("SELECT COUNT(*), COALESCE(SUM(precio), 0) FROM ventas");
        rs.next();
        int cantidadVentas = rs.getInt(1);
        double totalVendido = rs.getDouble(2);

        Map<String, Object> res = new HashMap<>();
        res.put("cantidadVentas", cantidadVentas);
        res.put("totalVendido", totalVendido);

        con.close();
        return res;
    }
}