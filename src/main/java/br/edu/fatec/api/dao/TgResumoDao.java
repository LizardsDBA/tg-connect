    package br.edu.fatec.api.dao;

    import java.util.Optional;

    public interface TgResumoDao {
        Optional<ResumoDto> findByTrabalhoId(long trabalhoId);
        boolean upsert(long trabalhoId, String resumoMd, String kpisJson);

        record ResumoDto(long trabalhoId, String resumoMd, String kpisJson) {}
    }
