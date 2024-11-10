package io.fiap.revenda.veiculos.driven.repository;

import io.fiap.revenda.veiculos.driven.domain.Documento;
import io.fiap.revenda.veiculos.driven.domain.ImmutableDocumento;
import io.fiap.revenda.veiculos.driven.domain.ImmutableDocumentoPessoa;
import io.fiap.revenda.veiculos.driven.domain.ImmutablePessoa;
import io.fiap.revenda.veiculos.driven.domain.ImmutableVeiculo;
import io.fiap.revenda.veiculos.driven.domain.Pessoa;
import io.fiap.revenda.veiculos.driven.domain.Veiculo;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
public class DocumentoRepository {
    private static final String TABLE_NAME = "documentos_tb";

    private final DynamoDbAsyncClient client;

    public DocumentoRepository(DynamoDbAsyncClient client) {
        this.client = client;
    }

    public Mono<Void> save(Documento documento) {
        var atributos = new HashMap<String, AttributeValueUpdate>();
        atributos.put("ORGAO",
            AttributeValueUpdate.builder().value(v -> v.s(documento.getOrgao()).build()).build());
        atributos.put("TIPO",
            AttributeValueUpdate.builder().value(v -> v.s(documento.getTipo()).build()).build());
        atributos.put("EMITIDO",
            AttributeValueUpdate.builder().value(v -> v.s(documento.getEmitido().toString()).build()).build());

        atributos.put("PESSOA",
            AttributeValueUpdate.builder().value(v -> v.m(mapPessoa(documento.getPessoa())).build()).build());

        atributos.put("VEICULO",
            AttributeValueUpdate.builder().value(v -> v.m(mapVeiculo(documento.getVeiculo())).build()).build());

        var request = UpdateItemRequest.builder()
            .attributeUpdates(atributos)
            .tableName(TABLE_NAME)
            .key(Map.of("ID", AttributeValue.fromS(documento.getId())))
            .build();

        return Mono.fromFuture(client.updateItem(request))
            .then();
    }

    public Mono<Void> deleteById(String id) {
        var key = new HashMap<String, AttributeValue>();
        key.put("ID", AttributeValue.fromS(id));

        var request = DeleteItemRequest.builder()
            .key(key)
            .tableName(TABLE_NAME)
            .build();

        return Mono.fromFuture(client.deleteItem(request))
            .then();
    }

    public Flux<Documento> fetch(Boolean emitido) {
        var request = QueryRequest.builder()
            .tableName(TABLE_NAME)
            .indexName("EmitidoIndex")
            .keyConditionExpression("#emitido = :emitido")
            .expressionAttributeNames(Map.of("#emitido", "EMITIDO"))
            .expressionAttributeValues(Map.of(":emitido", AttributeValue.fromS(emitido.toString())))
            .build();

        return Mono.fromFuture(client.query(request))
            .filter(QueryResponse::hasItems)
            .map(response -> response.items()
                .stream()
                .map(this::convertItem)
                .toList()
            )
            .flatMapIterable(l -> l);
    }

    public Mono<Documento> fetchById(String id) {
        var request = QueryRequest.builder()
            .tableName(TABLE_NAME)
            .keyConditionExpression("#id = :id")
            .expressionAttributeNames(Map.of("#id", "ID"))
            .expressionAttributeValues(Map.of(":id", AttributeValue.fromS(id)))
            .build();

        return Mono.fromFuture(client.query(request))
            .filter(QueryResponse::hasItems)
            .map(response -> response.items().get(0))
            .map(this::convertItem);
    }

    private HashMap<String, AttributeValue> mapPessoa(Pessoa pessoa) {
        var pessoaAttr = new HashMap<String, AttributeValue>();
        pessoaAttr.put("NOME", AttributeValue.builder().s(pessoa.getNome()).build());
        pessoaAttr.put("SOBRENOME", AttributeValue.builder().s(pessoa.getSobrenome()).build());

        var documentoPessoaAttr = new HashMap<String, AttributeValue>();
        documentoPessoaAttr.put("TIPO", AttributeValue.builder().s(pessoa.getDocumento().getTipo()).build());
        documentoPessoaAttr.put("VALOR", AttributeValue.builder().s(pessoa.getDocumento().getValor()).build());
        documentoPessoaAttr.put("EXPIRACAO",
            AttributeValue.builder().s(String.valueOf(pessoa.getDocumento().getExpiracao().toEpochDay())).build());

        pessoaAttr.put("DOCUMENTO", AttributeValue.builder().m(documentoPessoaAttr).build());

        return pessoaAttr;
    }

    private HashMap<String, AttributeValue> mapVeiculo(Veiculo veiculo) {
        var atributos = new HashMap<String, AttributeValue>();
        atributos.put("COR",
            AttributeValue.builder().s(veiculo.getCor()).build());
        atributos.put("ANO",
            AttributeValue.builder().s(veiculo.getAno()).build());
        atributos.put("MARCA",
            AttributeValue.builder().s(veiculo.getMarca()).build());
        atributos.put("PLACA",
            AttributeValue.builder().s(veiculo.getPlaca()).build());
        atributos.put("CAMBIO",
            AttributeValue.builder().s(veiculo.getCambio()).build());
        atributos.put("RENAVAM",
            AttributeValue.builder().s(veiculo.getRenavam()).build());
        atributos.put("MODELO",
            AttributeValue.builder().s(veiculo.getModelo()).build());

        return atributos;
    }

    private Documento convertItem(Map<String, AttributeValue> item) {
        var veiculo = ImmutableVeiculo.builder()
            .id(item.get("ID").s())
            .ano(item.get("ANO").s())
            .cor(item.get("COR").s())
            .marca(item.get("MARCA").s())
            .cambio(item.get("CAMBIO").s())
            .modelo(item.get("MODELO").s())
            .build();

        var pessoa = ImmutablePessoa.builder()
            .id(item.get("ID").s())
            .nome(item.get("NOME").s())
            .sobrenome(item.get("SOBRENOME").s())
            .documento(ImmutableDocumentoPessoa.builder()
                .tipo(item.get("DOCUMENTO").m().get("TIPO").s())
                .valor(item.get("DOCUMENTO").m().get("VALOR").s())
                .expiracao(LocalDate.ofEpochDay(
                        Long.parseLong(item.get("DOCUMENTO").m().get("EXPIRACAO").s())
                    )
                )
                .build()
            )
            .build();

        return ImmutableDocumento.builder()
            .id(item.get("ID").s())
            .tipo(item.get("TIPO").s())
            .orgao(item.get("ORGAO").s())
            .emitido(Boolean.valueOf(item.get("EMITIDO").s()))
            .pessoa(pessoa)
            .veiculo(veiculo)
            .build();
    }
}
