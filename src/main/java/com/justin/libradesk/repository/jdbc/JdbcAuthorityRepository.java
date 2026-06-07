package com.justin.libradesk.repository.jdbc;

import com.justin.libradesk.infrastructure.database.DatabaseManager;
import com.justin.libradesk.repository.AuthorityRepository;
import com.justin.libradesk.repository.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAuthorityRepository implements AuthorityRepository {

    private final DatabaseManager db;

    public JdbcAuthorityRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void addAuthorVariant(long authorId, String variantForm) {
        addVariant("author_variants", "author_id", authorId, variantForm);
    }

    @Override
    public Optional<Long> findAuthorIdByVariant(String variantForm) {
        return findParentByVariant("author_variants", "author_id", variantForm);
    }

    @Override
    public List<String> listAuthorVariants(long authorId) {
        return listVariants("author_variants", "author_id", authorId);
    }

    @Override
    public void addSubjectVariant(long subjectId, String variantForm) {
        addVariant("subject_variants", "subject_id", subjectId, variantForm);
    }

    @Override
    public Optional<Long> findSubjectIdByVariant(String variantForm) {
        return findParentByVariant("subject_variants", "subject_id", variantForm);
    }

    @Override
    public List<String> listSubjectVariants(long subjectId) {
        return listVariants("subject_variants", "subject_id", subjectId);
    }

    private void addVariant(String table, String parentColumn, long parentId, String variantForm) {
        String sql = "INSERT INTO " + table + " (" + parentColumn + ", variant_form) VALUES (?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, parentId);
            ps.setString(2, variantForm);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add variant '" + variantForm + "' to " + table, e);
        }
    }

    private Optional<Long> findParentByVariant(String table, String parentColumn, String variantForm) {
        String sql = "SELECT " + parentColumn + " FROM " + table
                + " WHERE lower(variant_form) = lower(?) LIMIT 1";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, variantForm);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong(parentColumn)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to resolve variant in " + table, e);
        }
    }

    private List<String> listVariants(String table, String parentColumn, long parentId) {
        List<String> forms = new ArrayList<>();
        String sql = "SELECT variant_form FROM " + table + " WHERE " + parentColumn
                + " = ? ORDER BY variant_form";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, parentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    forms.add(rs.getString("variant_form"));
                }
            }
            return forms;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to list variants in " + table, e);
        }
    }
}
