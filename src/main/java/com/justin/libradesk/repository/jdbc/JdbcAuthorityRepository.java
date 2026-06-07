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

    @Override
    public void mergeAuthor(long fromId, long intoId) {
        merge("book_authors", "author_id", "author_variants", "authors", fromId, intoId);
    }

    @Override
    public void mergeSubject(long fromId, long intoId) {
        merge("book_subjects", "subject_id", "subject_variants", "subjects", fromId, intoId);
    }

    /** Repoints a heading's bib links and variants onto another, then deletes the source. */
    private void merge(String linkTable, String linkColumn, String variantTable, String headingTable,
                       long fromId, long intoId) {
        try (Connection c = db.getConnection()) {
            boolean previousAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                // Drop links/variants that would collide with the target, then repoint the rest.
                update(c, "DELETE FROM " + linkTable + " WHERE " + linkColumn + " = ? AND book_id IN "
                        + "(SELECT book_id FROM " + linkTable + " WHERE " + linkColumn + " = ?)", fromId, intoId);
                update(c, "UPDATE " + linkTable + " SET " + linkColumn + " = ? WHERE " + linkColumn + " = ?",
                        intoId, fromId);
                update(c, "DELETE FROM " + variantTable + " WHERE " + linkColumn + " = ? AND lower(variant_form) IN "
                        + "(SELECT lower(variant_form) FROM " + variantTable + " WHERE " + linkColumn + " = ?)",
                        fromId, intoId);
                update(c, "UPDATE " + variantTable + " SET " + linkColumn + " = ? WHERE " + linkColumn + " = ?",
                        intoId, fromId);
                update(c, "DELETE FROM " + headingTable + " WHERE id = ?", fromId);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to merge " + headingTable + " " + fromId + " into " + intoId, e);
        }
    }

    private void update(Connection c, String sql, long first, long... rest) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, first);
            for (int i = 0; i < rest.length; i++) {
                ps.setLong(i + 2, rest[i]);
            }
            ps.executeUpdate();
        }
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
