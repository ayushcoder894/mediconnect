import { useCallback, useEffect, useState } from "react";
import { api } from "../api/client";

/** Loads a GET endpoint and exposes data, loading, error and a reload function. */
export function useApi(path, { skip = false } = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(!skip);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    if (!path || skip) return;
    setLoading(true);
    setError(null);
    try {
      setData(await api.get(path));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }, [path, skip]);

  useEffect(() => {
    load();
  }, [load]);

  return { data, loading, error, reload: load, setData };
}
