package com.annotation.service;

import com.annotation.model.Annotation;
import com.annotation.model.Dataset;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final AnnotationService annotationService;
    private final ObjectMapper objectMapper;

    public byte[] exporterCSV(Dataset dataset) throws Exception {
        List<Annotation> annotations = annotationService.findByDataset(dataset);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);

        // En-tête dynamique selon le nombre de textes par exemple
        int nbTextes = dataset.getNombreTextesParExemple();
        StringBuilder header = new StringBuilder("id");
        for (int i = 1; i <= nbTextes; i++) {
            header.append(",texte").append(i);
        }
        header.append(",classe,annotateur,date_annotation");
        writer.println(header);

        for (Annotation a : annotations) {
            StringBuilder line = new StringBuilder();
            line.append(a.getExemple().getId());
            List<String> textes = a.getExemple().getTextes();
            for (int i = 0; i < nbTextes; i++) {
                String t = i < textes.size() ? textes.get(i).replace("\"", "\"\"") : "";
                line.append(",\"").append(t).append("\"");
            }
            line.append(",\"").append(a.getClasseChoisie()).append("\"");
            line.append(",\"").append(a.getAnnotateur().getLogin()).append("\"");
            line.append(",\"").append(a.getDateAnnotation()).append("\"");
            writer.println(line);
        }
        writer.flush();
        return baos.toByteArray();
    }

    public byte[] exporterJSON(Dataset dataset) throws Exception {
        List<Annotation> annotations = annotationService.findByDataset(dataset);
        ArrayNode array = objectMapper.createArrayNode();

        for (Annotation a : annotations) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("id", a.getExemple().getId());

            // Textes comme tableau
            ArrayNode textesNode = node.putArray("textes");
            for (String t : a.getExemple().getTextes()) {
                textesNode.add(t);
            }

            node.put("classe", a.getClasseChoisie());
            node.put("annotateur", a.getAnnotateur().getLogin());
            node.put("date_annotation", a.getDateAnnotation().toString());
            array.add(node);
        }

        return objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsBytes(array);
    }
}
