package com.despensa.despensa;

import org.springframework.web.bind.annotation.*;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/productos")
public class ProductoController {

    // === AQUÍ PEGAS TUS DATOS DE NEON ===
    private final String url = "jdbc:postgresql://ep-floral-firefly-apnwelcp-pooler.c-7.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require";
    private final String user = "neondb_owner";
    private final String pass = "npg_63PKdETYFBCI";

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(url, user, pass);
    }

    private void inicializarBaseDatos() {
        try (Connection con = getConnection();
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
    public List<Map<String, Object>> productos() {
        inicializarBaseDatos();
        List<Map<String, Object>> lista = new ArrayList<>();

        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM productos ORDER BY id DESC")) {

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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    @PostMapping("/nuevo")
    public String nuevoProducto(@RequestBody Map<String, Object> body) {
        inicializarBaseDatos();

        String nombre = body.get("nombre").toString();
        String categoria = body.getOrDefault("categoria", "Gral").toString();
        double precio = Double.parseDouble(body.get("precio").toString());
        int stock = Integer.parseInt(body.get("stock").toString());
        int minimo = body.containsKey("minimo") && !body.get("minimo").toString().isEmpty()
                ? Integer.parseInt(body.get("minimo").toString()) : 2;

        try (Connection con = getConnection();
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
            return "OK";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        inicializarBaseDatos();
        Map<String, Object> res = new HashMap<>();

        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            ResultSet rs1 = st.executeQuery("SELECT COUNT(*), COALESCE(SUM(stock_actual), 0) FROM productos");
            if (rs1.next()) {
                res.put("totalProductos", rs1.getInt(1));
                res.put("stockTotal", rs1.getInt(2));
            }

            ResultSet rs2 = st.executeQuery("SELECT COUNT(*) FROM productos WHERE stock_actual <= stock_minimo");
            if (rs2.next()) {
                res.put("stockBajo", rs2.getInt(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.put("totalProductos", 0);
            res.put("stockTotal", 0);
            res.put("stockBajo", 0);
        }

        return res;
    }

    @GetMapping("/resumenVentas")
    public Map<String, Object> resumenVentas() {
        inicializarBaseDatos();
        Map<String, Object> res = new HashMap<>();

        try (Connection con = getConnection();
             Statement st = con.createStatement()) {

            ResultSet rs = st.executeQuery("SELECT COUNT(*), COALESCE(SUM(precio), 0) FROM ventas");
            if (rs.next()) {
                res.put("cantidadVentas", rs.getInt(1));
                res.put("totalVendido", rs.getDouble(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.put("cantidadVentas", 0);
            res.put("totalVendido", 0.0);
        }

        return res;
    }
}