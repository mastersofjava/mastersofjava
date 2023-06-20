{{- define "postgres.service.name" }}
{{- printf "%s-%s" .Release.Name "postgres" }}
{{- end }}

{{- define "postgres.service.port" }}
{{- 5432 }}
{{- end }}

{{- define "controller.service.name" }}
{{- printf "%s-%s" .Release.Name "controller" }}
{{- end }}

{{- define "controller.service.port" }}
{{- 8080 }}
{{- end }}

{{- define "controller.service.queuePort" }}
{{- 61616 }}
{{- end }}