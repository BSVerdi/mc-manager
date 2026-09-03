import { Save, X } from "lucide-react";
import type { Props } from "../../pages/FileViewer";

export function FileEditor({ editor }: Props) {
  if (!editor.file) return null;

  return (
    <section className="file-editor-container">
      <div className="file-editor-header">
        <span className="file-editor-path">{editor.file.path}</span>
        {editor.isDirty && (
          <span className="file-editor-dirty">● Não salvo</span>
        )}
        <div className="file-editor-actions">
          <button
            className="btn"
            disabled={!editor.isDirty || editor.saving}
            onClick={editor.save}
          >
            <Save
              style={{
                width: 18,
                height: 18,
                color: !editor.isDirty || editor.saving ? "#6b7280" : "#f0c674",
              }}
            />
          </button>
          <button onClick={editor.close}>
            <X style={{ width: 18, height: 18, color: "#ff0000" }} />
          </button>
        </div>
      </div>

      {editor.error && <p className="file-editor-error">{editor.error}</p>}

      {!editor.file.editable ? (
        <p className="file-editor-blocked">
          Este arquivo não pode ser editado por aqui (binário ou muito grande).
        </p>
      ) : (
        <textarea
          className="file-editor-textarea"
          value={editor.draft}
          onChange={(e) => editor.setDraft(e.target.value)}
          spellCheck={false}
        />
      )}
    </section>
  );
}
