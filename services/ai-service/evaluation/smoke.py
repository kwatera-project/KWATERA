"""Opt-in REST contract/recovery check against the existing Compose AI container."""
import argparse
import json
import pathlib
import subprocess
import sys
import time


def request(path, body=None, token=True):
    command = ['docker', 'exec', '-i', 'kwatera-ai-service', 'sh', '-c',
               'curl -sS --max-time 150 -w "\\n%{http_code}" '
               + ('-H "X-Internal-Token: $KWATERA_INTERNAL_TOKEN" ' if token else '')
               + ('-H "Content-Type: application/json" --data-binary @- ' if body is not None else '')
               + '"http://localhost:$AI_SERVICE_PORT' + path + '"']
    start = time.perf_counter()
    response = subprocess.run(command, input=json.dumps(body, ensure_ascii=False) if body else None,
                              text=True, encoding='utf-8', capture_output=True, check=True).stdout
    content, status = response.rsplit('\n', 1)
    return {'status': int(status), 'body': json.loads(content),
            'seconds': round(time.perf_counter() - start, 3)}


def main():
    sys.stdout.reconfigure(encoding='utf-8')
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=pathlib.Path, required=True)
    parser.add_argument('--outage', action='store_true', help='Briefly stop and restart ONLY Ollama')
    args = parser.parse_args()
    rows = []

    def check(label, path, body=None, expected=200, token=True):
        row = {'case': label, **request(path, body, token)}
        rows.append(row)
        args.output.write_text(json.dumps(rows, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        print(label, row['status'], row['seconds'], flush=True)
        allowed = expected if isinstance(expected, tuple) else (expected,)
        assert row['status'] in allowed, row
        if expected == (503, 504):
            assert row['body']['code'] in ('AI_UNAVAILABLE', 'AI_TIMEOUT'), row

    translate = '/internal/ai/translate'
    summarize = '/internal/ai/summarize'
    original = {'text': 'Hello, what time can I check in?', 'sourceLanguage': 'en', 'targetLanguage': 'pl'}
    reviews = {'reviews': ['Clean room and friendly host.', 'Wi-Fi was unstable.'], 'language': 'en'}
    check('translate-explicit', translate, original)
    check('summary-independent', summarize, reviews)
    for label, text, target in [('auto-en', original['text'], 'pl'),
                                 ('auto-pl', 'Czy mogę się wymeldować o 11:30?', 'en'),
                                 ('auto-fr', 'La caution sera-t-elle remboursée après notre départ ?', 'en')]:
        check(label, translate, {'text': text, 'targetLanguage': target})
    check('same-language', translate, {'text': 'Hello\n\nŁ-0042', 'sourceLanguage': 'EN-us', 'targetLanguage': 'en-US'})
    assert rows[-1]['body']['translatedText'] == 'Hello\n\nŁ-0042'
    check('missing-token', translate, original, 403, False)
    if args.outage:
        try:
            subprocess.run(['docker', 'stop', 'kwatera-ollama'], check=True, capture_output=True)
            check('outage-translate', translate, original, (503, 504))
            check('outage-summary', summarize, reviews, (503, 504))
            check('outage-liveness', '/actuator/health/liveness')
            check('outage-readiness', '/actuator/health/readiness', expected=503)
        finally:
            subprocess.run(['docker', 'start', 'kwatera-ollama'], check=True, capture_output=True)
        for _ in range(30):
            if request('/actuator/health/readiness')['status'] == 200:
                break
            time.sleep(1)
        check('recovered-readiness', '/actuator/health/readiness')
        check('recovered-translate', translate, original)
        check('recovered-summary', summarize, reviews)


if __name__ == '__main__':
    main()
