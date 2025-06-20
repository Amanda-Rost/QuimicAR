package test;

import static org.junit.Assert.assertEquals;

import com.google.ar.core.examples.java.principal.rendering.Composto;
import com.google.ar.core.examples.java.principal.rendering.Composto.BufferData;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class TesteComposto {

    private Composto composto;

    @Before
    public void setUp() {
        composto = new Composto();
    }

    @Test
    public void testFormato() {
        String formato = "FormatoTeste";
        composto.setFormato(formato);
        assertEquals(formato, composto.getFormato());
    }

    @Test
    public void testNomenclatura() {
        String nomenclatura = "NomenclaturaTeste";
        composto.setNomenclatura(nomenclatura);
        assertEquals(nomenclatura, composto.getNomenclatura());
    }

    @Test
    public void testObjeto3D() {
        String objeto3DPath = "models/metano.obj";
        composto.setObjeto3D(objeto3DPath);
        assertEquals(objeto3DPath, composto.getObjeto3D());
    }

    @Test
    public void testConfig3D() {
        BufferData config3D = new BufferData();
        config3D.setType("Buffer");
        config3D.setData(Arrays.asList(109, 111, 100, 101, 108, 115, 47, 109, 101, 116, 97, 110, 111, 46, 109, 116, 108));
        composto.setConfig3D(config3D);
        assertEquals("models/metano.mtl", composto.getConfig3D().getDataAsString());
    }

    @Test
    public void testTextura() {
        BufferData textura = new BufferData();
        textura.setType("Buffer");
        textura.setData(Arrays.asList(109, 111, 100, 101, 108, 115, 47, 112, 114, 101, 116, 111, 98, 114, 97, 110, 99, 111, 46, 112, 110, 103));
        composto.setTextura(textura);
        assertEquals("models/pretobranco.png", composto.getTextura().getDataAsString());
    }
}