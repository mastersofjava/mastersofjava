{{- define "postgres.service.name" }}
{{- printf "%s-%s" .Release.Name "postgres" }}
{{- end }}

{{- define "controller.service.name" }}
{{- printf "%s-%s" .Release.Name "controller" }}
{{- end }}

