{{/*
Define the full name
*/}}
{{- define "postgres.fullname" }}
{{- printf "%s-%s" .Release.Name "postgres" }}
{{- end }}

{{- define "postgres.service.name" }}
{{- printf "%s-%s" .Release.Name "postgres" }}
{{- end }}

{{- define "postgres.service.port" }}
{{- .Values.service.port }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "postgres.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "postgres.labels" -}}
helm.sh/chart: {{ include "postgres.chart" . }}
{{ include "postgres.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/part-of: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "postgres.selectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: database
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "postgres.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "postgres.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{- define "postgres.datatbases" -}}
{{- printf "iam:%s:%s,controller:%s:%s"
    (default "iam" .Values.global.postgres.databases.iam.username)
    (default "iam" .Values.global.postgres.databases.iam.password)
    (default "controller" .Values.global.postgres.databases.controller.username)
    (default "controller" .Values.global.postgres.databases.controller.password) -}}
{{- end }}