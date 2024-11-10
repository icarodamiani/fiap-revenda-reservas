package io.fiap.revenda.veiculos.driven.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import io.fiap.revenda.veiculos.driven.domain.Documento;
import io.fiap.revenda.veiculos.driven.repository.DocumentoRepository;
import io.fiap.revenda.veiculos.driven.client.SqsMessageClient;
import io.fiap.revenda.veiculos.driven.client.dto.EmissaoDocumentoRequestDTO;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedFunction2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Service
public class DocumentoService {
    private final String queue;
    private final DocumentoRepository repository;
    private final SqsMessageClient messageClient;
    private final ObjectMapper objectMapper;

    public DocumentoService(@Value("${aws.sqs.veiculosUpdate.queue}")
                          String queue,
                            DocumentoRepository repository,
                            SqsMessageClient messageClient,
                            ObjectMapper objectMapper) {
        this.repository = repository;
        this.messageClient = messageClient;
        this.queue = queue;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> save(Documento pessoa) {
        return repository.save(pessoa);
    }

    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    public Flux<Documento> fetch(Boolean vendido) {
        return repository.fetch(vendido);
    }

    public Mono<Documento> fetchById(String id) {
        return repository.fetchById(id);
    }

    public Flux<DeleteMessageResponse> handleEmissaoDocumento() {
        return messageClient.receive(queue)
            .filter(ReceiveMessageResponse::hasMessages)
            .flatMapIterable(ReceiveMessageResponse::messages)
            .flatMap(message ->
                Mono.fromSupplier(() -> {
                        try {
                            return objectMapper.readValue(message.body(), EmissaoDocumentoRequestDTO.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }).flatMap(veiculoUpdate ->
                        this.fetchById(veiculoUpdate.getId())
                            .map(veiculo -> applyPatch().unchecked().apply(veiculoUpdate.getPatch(), veiculo))
                            .map(node -> convertToVeiculo().unchecked().apply(node))
                    )
                    .flatMap(this::save)
                    .flatMap(unused -> messageClient.delete(queue, message))
            );
    }

    private CheckedFunction2<JsonPatch, Documento, JsonNode> applyPatch() {
        return (patch, veiculo) -> patch.apply(objectMapper.convertValue(veiculo, JsonNode.class));
    }

    private CheckedFunction1<JsonNode, Documento> convertToVeiculo() {
        return node -> objectMapper.treeToValue(node, Documento.class);
    }
}
