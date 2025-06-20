package com.google.ar.core.examples.java.principal.rendering;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Composto {
    private String nomenclatura;
    private String formato;

    @SerializedName("3D")
    private String objeto3D; // Caminho para o arquivo do modelo 3D

    private BufferData config3D; // Alterado para BufferData
    private BufferData textura;  // Alterado para BufferData

    // Classe interna para representar a estrutura dos campos config3D e textura
    public static class BufferData {
        private String type;
        private List<Integer> data;

        // Getters e setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<Integer> getData() {
            return data;
        }

        public void setData(List<Integer> data) {
            this.data = data;
        }

        // Método para converter a lista de inteiros em uma string
        public String getDataAsString() {
            StringBuilder sb = new StringBuilder();
            for (Integer byteValue : data) {
                sb.append((char) byteValue.intValue());
            }
            return sb.toString();
        }
    }

    // Getters e setters para Composto
    public String getNomenclatura() {
        return nomenclatura;
    }

    public void setNomenclatura(String nomenclatura) {
        this.nomenclatura = nomenclatura;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getObjeto3D() {
        return objeto3D;
    }

    public void setObjeto3D(String objeto3D) {
        this.objeto3D = objeto3D;
    }

    public BufferData getConfig3D() {
        return config3D;
    }

    public void setConfig3D(BufferData config3D) {
        this.config3D = config3D;
    }

    public BufferData getTextura() {
        return textura;
    }

    public void setTextura(BufferData textura) {
        this.textura = textura;
    }
}