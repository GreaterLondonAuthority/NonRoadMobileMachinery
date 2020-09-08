--
-- PostgreSQL database dump
--

-- Dumped from database version 11.5
-- Dumped by pg_dump version 12.0

-- Started on 2020-08-20 10:00:03 BST

--
-- TOC entry 10 (class 2615 OID 45613)
-- Name: nrmm; Type: SCHEMA; Schema: -; Owner: nrmm
--

--CREATE SCHEMA nrmm;


--ALTER SCHEMA nrmm OWNER TO nrmm;

--
-- TOC entry 1673 (class 1255 OID 88731785)
-- Name: action_hit(); Type: FUNCTION; Schema: nrmm; Owner: nrmm
--

CREATE FUNCTION nrmm.action_hit() RETURNS trigger
    LANGUAGE plpgsql
    AS $$begin
    new.used := false;
	new.hit_count := new.hit_count + 1;
    return new;
end
$$;


ALTER FUNCTION nrmm.action_hit() OWNER TO nrmm;

--
-- TOC entry 1680 (class 1255 OID 88716567)
-- Name: machinery_admin_user_id(); Type: FUNCTION; Schema: nrmm; Owner: nrmm
--

CREATE FUNCTION nrmm.machinery_admin_user_id() RETURNS trigger
    LANGUAGE plpgsql
    AS $$begin
    new.user_id := new.admin_user_id;
    return new;
end
$$;


ALTER FUNCTION nrmm.machinery_admin_user_id() OWNER TO nrmm;

--
-- TOC entry 1677 (class 1255 OID 45614)
-- Name: set_time_added(); Type: FUNCTION; Schema: nrmm; Owner: nrmm
--

CREATE FUNCTION nrmm.set_time_added() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN NEW.time_added = now(); RETURN NEW; END; $$;


ALTER FUNCTION nrmm.set_time_added() OWNER TO nrmm;

--
-- TOC entry 1678 (class 1255 OID 45615)
-- Name: set_time_modified(); Type: FUNCTION; Schema: nrmm; Owner: nrmm
--

CREATE FUNCTION nrmm.set_time_modified() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN NEW.time_modified = now(); RETURN NEW; END; $$;


ALTER FUNCTION nrmm.set_time_modified() OWNER TO nrmm;

--
-- TOC entry 1679 (class 1255 OID 45616)
-- Name: users_username_clean(); Type: FUNCTION; Schema: nrmm; Owner: nrmm
--

CREATE FUNCTION nrmm.users_username_clean() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ BEGIN NEW.username = trim(NEW.username); RETURN NEW; END; $$;


ALTER FUNCTION nrmm.users_username_clean() OWNER TO nrmm;

--
-- TOC entry 285 (class 1259 OID 45617)
-- Name: action_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.action_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.action_id_seq OWNER TO nrmm;

SET default_tablespace = '';

--
-- TOC entry 286 (class 1259 OID 45619)
-- Name: action; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.action (
    id integer DEFAULT nextval('nrmm.action_id_seq'::regclass) NOT NULL,
    expiry timestamp without time zone,
    action_type_id integer NOT NULL,
    settings text,
    guid text NOT NULL,
    used boolean,
    user_id integer,
    tag text,
    hit_count integer DEFAULT 0 NOT NULL
);


ALTER TABLE nrmm.action OWNER TO nrmm;

--
-- TOC entry 287 (class 1259 OID 45626)
-- Name: action_type_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.action_type_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.action_type_id_seq OWNER TO nrmm;

--
-- TOC entry 288 (class 1259 OID 45628)
-- Name: action_type; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.action_type (
    id integer DEFAULT nextval('nrmm.action_type_id_seq'::regclass) NOT NULL,
    name character varying(200) NOT NULL,
    description character varying(500) DEFAULT NULL::character varying,
    disabled boolean DEFAULT false,
    workflow_id integer NOT NULL
);


ALTER TABLE nrmm.action_type OWNER TO nrmm;

--
-- TOC entry 289 (class 1259 OID 45637)
-- Name: auto_save; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.auto_save (
    id integer NOT NULL,
    users_id integer,
    reference_id integer NOT NULL,
    reference_type character varying(40) NOT NULL,
    time_added timestamp without time zone DEFAULT now() NOT NULL,
    saved_values text
);


ALTER TABLE nrmm.auto_save OWNER TO nrmm;

--
-- TOC entry 290 (class 1259 OID 45644)
-- Name: auto_save_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.auto_save_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.auto_save_id_seq OWNER TO nrmm;

--
-- TOC entry 6097 (class 0 OID 0)
-- Dependencies: 290
-- Name: auto_save_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.auto_save_id_seq OWNED BY nrmm.auto_save.id;


--
-- TOC entry 291 (class 1259 OID 45646)
-- Name: borough_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.borough_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.borough_id_seq OWNER TO nrmm;

--
-- TOC entry 292 (class 1259 OID 45648)
-- Name: borough; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.borough (
    id integer DEFAULT nextval('nrmm.borough_id_seq'::regclass) NOT NULL,
    name text NOT NULL,
    disabled boolean DEFAULT false
);


ALTER TABLE nrmm.borough OWNER TO nrmm;

--
-- TOC entry 293 (class 1259 OID 45656)
-- Name: media_type; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.media_type (
    id integer NOT NULL,
    name character varying(250) NOT NULL,
    description character varying(250) DEFAULT NULL::character varying,
    internal boolean DEFAULT false,
    disabled boolean DEFAULT false
);


ALTER TABLE nrmm.media_type OWNER TO nrmm;

--
-- TOC entry 294 (class 1259 OID 45665)
-- Name: case_media_type_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.case_media_type_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.case_media_type_id_seq OWNER TO nrmm;

--
-- TOC entry 6098 (class 0 OID 0)
-- Dependencies: 294
-- Name: case_media_type_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.case_media_type_id_seq OWNED BY nrmm.media_type.id;


--
-- TOC entry 295 (class 1259 OID 45667)
-- Name: change_log; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.change_log (
    id integer NOT NULL,
    time_added timestamp without time zone DEFAULT now() NOT NULL,
    change_type character varying(100) NOT NULL,
    user_full_name character varying(100) NOT NULL,
    table_affected character varying(100) NOT NULL,
    row_affected integer,
    previous_values text,
    parent_row character varying(100) DEFAULT NULL::character varying
);


ALTER TABLE nrmm.change_log OWNER TO nrmm;

--
-- TOC entry 296 (class 1259 OID 45675)
-- Name: change_log_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.change_log_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.change_log_id_seq OWNER TO nrmm;

--
-- TOC entry 6099 (class 0 OID 0)
-- Dependencies: 296
-- Name: change_log_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.change_log_id_seq OWNED BY nrmm.change_log.id;


--
-- TOC entry 297 (class 1259 OID 45677)
-- Name: datasource; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.datasource (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    driver character varying(255) NOT NULL,
    database_url character varying(255) NOT NULL,
    username character varying(100) NOT NULL,
    password character varying(100) DEFAULT NULL::character varying,
    use_connection_pool boolean DEFAULT false,
    remove_abandoned boolean DEFAULT false,
    validation_query character varying(200) DEFAULT NULL::character varying,
    max_active smallint,
    max_idle smallint,
    min_idle smallint,
    initial_size smallint,
    max_wait integer,
    remove_abandoned_timeout smallint,
    init_sql text,
    use_cache boolean DEFAULT false,
    cache_timeout smallint,
    cache_trigger_report_id integer
);


ALTER TABLE nrmm.datasource OWNER TO nrmm;

--
-- TOC entry 298 (class 1259 OID 45688)
-- Name: datasource_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.datasource_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.datasource_id_seq OWNER TO nrmm;

--
-- TOC entry 6100 (class 0 OID 0)
-- Dependencies: 298
-- Name: datasource_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.datasource_id_seq OWNED BY nrmm.datasource.id;


--
-- TOC entry 299 (class 1259 OID 45690)
-- Name: distribution_list; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.distribution_list (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    settings text,
    type_id character varying(20) DEFAULT 'email'::character varying NOT NULL,
    foreach boolean DEFAULT false,
    content text NOT NULL,
    username character varying(255) DEFAULT NULL::character varying,
    password character varying(255) DEFAULT NULL::character varying,
    secondary_content text,
    secondary_username character varying(255) DEFAULT NULL::character varying,
    secondary_password character varying(255) DEFAULT NULL::character varying,
    email_host character varying(255) DEFAULT NULL::character varying,
    email_cc text,
    email_bcc text,
    email_from character varying(255) DEFAULT NULL::character varying,
    email_subject character varying(255) DEFAULT NULL::character varying,
    email_body text,
    email_attachment_name character varying(255) DEFAULT NULL::character varying,
    email_importance character varying(100) DEFAULT NULL::character varying,
    email_priority character varying(100) DEFAULT NULL::character varying,
    email_sensitivity character varying(100) DEFAULT NULL::character varying,
    compression character varying(20) DEFAULT NULL::character varying,
    user_dir_is_root boolean DEFAULT false,
    datasource_id integer,
    pgp_encrypted_output boolean DEFAULT false,
    pgp_public_key bytea,
    ssh_user_authentication boolean DEFAULT false,
    ssh_key bytea,
    ssh_passphrase character varying(100) DEFAULT NULL::character varying,
    internal boolean DEFAULT false
);


ALTER TABLE nrmm.distribution_list OWNER TO nrmm;

--
-- TOC entry 300 (class 1259 OID 45715)
-- Name: distribution_list_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.distribution_list_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.distribution_list_id_seq OWNER TO nrmm;

--
-- TOC entry 6101 (class 0 OID 0)
-- Dependencies: 300
-- Name: distribution_list_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.distribution_list_id_seq OWNED BY nrmm.distribution_list.id;


--
-- TOC entry 345 (class 1259 OID 46207)
-- Name: email_queue_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.email_queue_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.email_queue_id_seq OWNER TO nrmm;

--
-- TOC entry 346 (class 1259 OID 46209)
-- Name: email_queue; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.email_queue (
    id integer DEFAULT nextval('nrmm.email_queue_id_seq'::regclass) NOT NULL,
    email_object text,
    email_to text,
    email_from text,
    email_subject text,
    time_added timestamp without time zone,
    send_attempts smallint DEFAULT 0 NOT NULL
);


ALTER TABLE nrmm.email_queue OWNER TO nrmm;

--
-- TOC entry 301 (class 1259 OID 45717)
-- Name: folder; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.folder (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    parent integer DEFAULT 0,
    internal boolean DEFAULT false
);


ALTER TABLE nrmm.folder OWNER TO nrmm;

--
-- TOC entry 302 (class 1259 OID 45722)
-- Name: folder_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.folder_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.folder_id_seq OWNER TO nrmm;

--
-- TOC entry 6102 (class 0 OID 0)
-- Dependencies: 302
-- Name: folder_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.folder_id_seq OWNED BY nrmm.folder.id;


--
-- TOC entry 303 (class 1259 OID 45724)
-- Name: log; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.log (
    id integer NOT NULL,
    task_id integer,
    task_name character varying(255) DEFAULT NULL::character varying,
    date_added timestamp without time zone DEFAULT now() NOT NULL,
    status character varying(255) NOT NULL,
    message text,
    duration bigint,
    total bigint,
    report_name character varying(255) DEFAULT NULL::character varying,
    parameter_values text,
    recipients text,
    server_id character varying(100) NOT NULL,
    user_full_name character varying(100) DEFAULT NULL::character varying,
    user_location character varying(100) DEFAULT NULL::character varying
);


ALTER TABLE nrmm.log OWNER TO nrmm;

--
-- TOC entry 304 (class 1259 OID 45735)
-- Name: log_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.log_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.log_id_seq OWNER TO nrmm;

--
-- TOC entry 6103 (class 0 OID 0)
-- Dependencies: 304
-- Name: log_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.log_id_seq OWNED BY nrmm.log.id;


--
-- TOC entry 305 (class 1259 OID 45737)
-- Name: lookups_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.lookups_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.lookups_id_seq OWNER TO nrmm;

--
-- TOC entry 306 (class 1259 OID 45739)
-- Name: lookups; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.lookups (
    id integer DEFAULT nextval('nrmm.lookups_id_seq'::regclass) NOT NULL,
    name text NOT NULL,
    description text,
    type text NOT NULL,
    tag text,
    disabled boolean DEFAULT false
);


ALTER TABLE nrmm.lookups OWNER TO nrmm;

--
-- TOC entry 307 (class 1259 OID 45747)
-- Name: machinery_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.machinery_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.machinery_id_seq OWNER TO nrmm;

--
-- TOC entry 308 (class 1259 OID 45749)
-- Name: machinery; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.machinery (
    id integer DEFAULT nextval('nrmm.machinery_id_seq'::regclass) NOT NULL,
    type_other text,
    contractor text,
    start_date timestamp without time zone NOT NULL,
    end_date timestamp without time zone NOT NULL,
    machine_id text NOT NULL,
    supplier text,
    engine_manufacturer text,
    machinery_manufacturer text,
    power_rating numeric,
    type_approval_number text,
    eu_stage_id integer,
    retrofit_model_id integer,
    retrofit_id text,
    added_by integer NOT NULL,
    modified_by integer NOT NULL,
    time_added timestamp without time zone,
    time_modified timestamp without time zone,
    type_id integer,
    retrofit_model_other text,
    exemption_reason_id integer,
    exemption_reason_text text,
    admin_user_id integer,
    exemption_status text,
    mig_userid integer,
    site_id integer,
    exemption_status_date timestamp without time zone,
    exemption_status_expiry_date timestamp without time zone,
    exemption_status_reason_id integer,
    exemption_id text,
    exemption_status_code_id integer,
    fixed text,
    user_id integer
);


ALTER TABLE nrmm.machinery OWNER TO nrmm;

--
-- TOC entry 309 (class 1259 OID 45756)
-- Name: machinery_media_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.machinery_media_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.machinery_media_id_seq OWNER TO nrmm;

--
-- TOC entry 310 (class 1259 OID 45758)
-- Name: machinery_media; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.machinery_media (
    id integer DEFAULT nextval('nrmm.machinery_media_id_seq'::regclass) NOT NULL,
    media_id integer NOT NULL,
    machinery_id integer
);


ALTER TABLE nrmm.machinery_media OWNER TO nrmm;

--
-- TOC entry 311 (class 1259 OID 45762)
-- Name: media; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.media (
    id integer NOT NULL,
    name text NOT NULL,
    description text,
    type character varying(100),
    folder character varying(300),
    extension character varying(10) DEFAULT NULL::character varying,
    file bytea,
    file_size integer,
    filename text,
    internal boolean DEFAULT false,
    time_added timestamp without time zone DEFAULT now() NOT NULL,
    time_modified timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE nrmm.media OWNER TO nrmm;

--
-- TOC entry 312 (class 1259 OID 45772)
-- Name: media_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.media_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.media_id_seq OWNER TO nrmm;

--
-- TOC entry 6104 (class 0 OID 0)
-- Dependencies: 312
-- Name: media_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.media_id_seq OWNED BY nrmm.media.id;


--
-- TOC entry 313 (class 1259 OID 45774)
-- Name: note; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.note (
    id integer NOT NULL,
    user_id integer,
    reference_id integer NOT NULL,
    reference_type character varying(40) NOT NULL,
    content text NOT NULL,
    folder character varying(300) NOT NULL,
    time_added timestamp without time zone DEFAULT now() NOT NULL,
    tag character varying(100) DEFAULT NULL::character varying,
    type_id integer,
    viewed boolean DEFAULT false,
    action_date timestamp without time zone,
    added_by integer,
    role_id integer,
    linked_only boolean DEFAULT true,
    plain_content text
);


ALTER TABLE nrmm.note OWNER TO nrmm;

--
-- TOC entry 314 (class 1259 OID 45784)
-- Name: note_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.note_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.note_id_seq OWNER TO nrmm;

--
-- TOC entry 6105 (class 0 OID 0)
-- Dependencies: 314
-- Name: note_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.note_id_seq OWNED BY nrmm.note.id;


--
-- TOC entry 315 (class 1259 OID 45786)
-- Name: note_media; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.note_media (
    id integer NOT NULL,
    media_id integer NOT NULL,
    note_id integer
);


ALTER TABLE nrmm.note_media OWNER TO nrmm;

--
-- TOC entry 316 (class 1259 OID 45789)
-- Name: note_media_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.note_media_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.note_media_id_seq OWNER TO nrmm;

--
-- TOC entry 6106 (class 0 OID 0)
-- Dependencies: 316
-- Name: note_media_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.note_media_id_seq OWNED BY nrmm.note_media.id;


--
-- TOC entry 317 (class 1259 OID 45791)
-- Name: note_type; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.note_type (
    id integer NOT NULL,
    name character varying(200) NOT NULL,
    description character varying(500) DEFAULT NULL::character varying,
    icon integer,
    disabled boolean DEFAULT false,
    internal boolean DEFAULT false
);


ALTER TABLE nrmm.note_type OWNER TO nrmm;

--
-- TOC entry 318 (class 1259 OID 45800)
-- Name: note_type_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.note_type_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.note_type_id_seq OWNER TO nrmm;

--
-- TOC entry 6107 (class 0 OID 0)
-- Dependencies: 318
-- Name: note_type_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.note_type_id_seq OWNED BY nrmm.note_type.id;


--
-- TOC entry 319 (class 1259 OID 45802)
-- Name: patch; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.patch (
    name character varying(255) NOT NULL,
    checksum character varying(255) NOT NULL,
    time_added timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE nrmm.patch OWNER TO nrmm;

--
-- TOC entry 320 (class 1259 OID 45809)
-- Name: report; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.report (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    file bytea NOT NULL,
    type character varying(255) DEFAULT 'Velocity'::character varying NOT NULL,
    time_modified timestamp without time zone DEFAULT now(),
    category character varying(100) DEFAULT NULL::character varying,
    internal boolean DEFAULT false
);


ALTER TABLE nrmm.report OWNER TO nrmm;

--
-- TOC entry 321 (class 1259 OID 45819)
-- Name: report_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.report_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.report_id_seq OWNER TO nrmm;

--
-- TOC entry 6108 (class 0 OID 0)
-- Dependencies: 321
-- Name: report_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.report_id_seq OWNED BY nrmm.report.id;


--
-- TOC entry 322 (class 1259 OID 45821)
-- Name: report_text; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.report_text (
    id integer NOT NULL,
    name character varying(200) NOT NULL,
    description character varying(500) DEFAULT NULL::character varying,
    type_id integer NOT NULL,
    text text NOT NULL,
    disabled boolean DEFAULT false,
    layout character varying(50) DEFAULT NULL::character varying
);


ALTER TABLE nrmm.report_text OWNER TO nrmm;

--
-- TOC entry 323 (class 1259 OID 45830)
-- Name: report_text_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.report_text_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.report_text_id_seq OWNER TO nrmm;

--
-- TOC entry 6109 (class 0 OID 0)
-- Dependencies: 323
-- Name: report_text_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.report_text_id_seq OWNED BY nrmm.report_text.id;


--
-- TOC entry 324 (class 1259 OID 45832)
-- Name: report_text_type; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.report_text_type (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    icon integer,
    internal boolean DEFAULT false,
    disabled boolean DEFAULT false
);


ALTER TABLE nrmm.report_text_type OWNER TO nrmm;

--
-- TOC entry 325 (class 1259 OID 45837)
-- Name: report_text_type_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.report_text_type_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.report_text_type_id_seq OWNER TO nrmm;

--
-- TOC entry 6110 (class 0 OID 0)
-- Dependencies: 325
-- Name: report_text_type_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.report_text_type_id_seq OWNED BY nrmm.report_text_type.id;


--
-- TOC entry 326 (class 1259 OID 45839)
-- Name: role; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.role (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    administrator boolean DEFAULT false,
    privileges character varying(1024) DEFAULT NULL::character varying,
    disabled boolean DEFAULT false,
    type_id integer,
    privilege_access character varying(1024) DEFAULT NULL::character varying
);


ALTER TABLE nrmm.role OWNER TO nrmm;

--
-- TOC entry 327 (class 1259 OID 45849)
-- Name: role_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.role_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.role_id_seq OWNER TO nrmm;

--
-- TOC entry 6111 (class 0 OID 0)
-- Dependencies: 327
-- Name: role_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.role_id_seq OWNED BY nrmm.role.id;


--
-- TOC entry 328 (class 1259 OID 45851)
-- Name: role_type; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.role_type (
    id integer NOT NULL,
    name character varying(250) NOT NULL,
    description character varying(250) DEFAULT NULL::character varying,
    internal boolean DEFAULT false,
    disabled boolean DEFAULT false
);


ALTER TABLE nrmm.role_type OWNER TO nrmm;

--
-- TOC entry 329 (class 1259 OID 45860)
-- Name: role_type_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.role_type_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.role_type_id_seq OWNER TO nrmm;

--
-- TOC entry 6112 (class 0 OID 0)
-- Dependencies: 329
-- Name: role_type_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.role_type_id_seq OWNED BY nrmm.role_type.id;


--
-- TOC entry 330 (class 1259 OID 45862)
-- Name: scheduled_task; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.scheduled_task (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    task_type character varying(20) DEFAULT 'report'::character varying NOT NULL,
    report_id integer,
    datasource_id integer NOT NULL,
    datasource_id1 integer,
    datasource_id2 integer,
    datasource_id3 integer,
    datasource_id4 integer,
    distribution_list_id integer,
    error_distribution_list_id integer,
    notify_distribution_list_id integer,
    disabled boolean DEFAULT false,
    locked boolean DEFAULT false,
    locked_by character varying(255) DEFAULT NULL::character varying,
    output_type character varying(20) DEFAULT NULL::character varying,
    sched_type integer DEFAULT 0 NOT NULL,
    sched_run_every_minutes integer DEFAULT 0,
    sched_run_every_minutes_from character varying(10) DEFAULT NULL::character varying,
    sched_run_every_minutes_to character varying(10) DEFAULT NULL::character varying,
    sched_run_every_day_at character varying(10) DEFAULT '0:00'::character varying,
    sched_run_every_day_exclude_weekends boolean DEFAULT true,
    sched_run_every_week_days character varying(255) DEFAULT NULL::character varying,
    sched_run_every_week_days_at character varying(10) DEFAULT '0:00'::character varying,
    sched_run_every_month_on integer DEFAULT 1,
    sched_run_once_on timestamp without time zone,
    sched_run_every_month_at character varying(10) DEFAULT '0:00'::character varying,
    sched_last_run timestamp without time zone,
    sched_last_error text,
    endpoint character varying(100) DEFAULT NULL::character varying,
    settings text,
    sched_run_every_day_exclude_holidays boolean DEFAULT true
);


ALTER TABLE nrmm.scheduled_task OWNER TO nrmm;

--
-- TOC entry 331 (class 1259 OID 45885)
-- Name: scheduled_task_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.scheduled_task_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.scheduled_task_id_seq OWNER TO nrmm;

--
-- TOC entry 6113 (class 0 OID 0)
-- Dependencies: 331
-- Name: scheduled_task_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.scheduled_task_id_seq OWNED BY nrmm.scheduled_task.id;


--
-- TOC entry 332 (class 1259 OID 45887)
-- Name: settings; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.settings (
    name character varying(100) NOT NULL,
    description text,
    value text,
    value_text text,
    value_numeric numeric(10,0) DEFAULT NULL::numeric
);


ALTER TABLE nrmm.settings OWNER TO nrmm;

--
-- TOC entry 333 (class 1259 OID 45894)
-- Name: site_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.site_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.site_id_seq OWNER TO nrmm;

--
-- TOC entry 334 (class 1259 OID 45896)
-- Name: site; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.site (
    id integer DEFAULT nextval('nrmm.site_id_seq'::regclass) NOT NULL,
    name text NOT NULL,
    description text,
    address text,
    postcode text,
    longitude text,
    latitude text,
    borough_id integer,
    zone text,
    planning_app_number text,
    contact_first_name text,
    contact_last_name text,
    contact_phone_number text,
    added_by integer,
    contact_email text,
    modified_by integer NOT NULL,
    start_date_new timestamp without time zone,
    time_added timestamp without time zone,
    time_modified timestamp without time zone,
    start_date timestamp without time zone,
    end_date timestamp without time zone,
    mig_userid integer
);


ALTER TABLE nrmm.site OWNER TO nrmm;

--
-- TOC entry 335 (class 1259 OID 45903)
-- Name: site_users; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.site_users (
    site_id integer NOT NULL,
    users_id integer NOT NULL,
    role_id integer
);


ALTER TABLE nrmm.site_users OWNER TO nrmm;

--
-- TOC entry 347 (class 1259 OID 83789809)
-- Name: stats_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.stats_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.stats_id_seq OWNER TO nrmm;

--
-- TOC entry 348 (class 1259 OID 83789811)
-- Name: stats; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.stats (
    id integer DEFAULT nextval('nrmm.stats_id_seq'::regclass) NOT NULL,
    time_added timestamp without time zone DEFAULT now() NOT NULL,
    type character varying(100) NOT NULL,
    name character varying(100) NOT NULL,
    value text
);


ALTER TABLE nrmm.stats OWNER TO nrmm;

--
-- TOC entry 336 (class 1259 OID 45906)
-- Name: user_log; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.user_log (
    id integer NOT NULL,
    userid character varying(50) NOT NULL,
    sessionid character varying(50) NOT NULL,
    ip_address character varying(100) DEFAULT NULL::character varying,
    user_agent character varying(1000) DEFAULT NULL::character varying,
    browser_locale character varying(100) DEFAULT NULL::character varying,
    browser character varying(100) DEFAULT NULL::character varying,
    browser_version character varying(100) DEFAULT NULL::character varying,
    os character varying(100) DEFAULT NULL::character varying,
    os_architecture character varying(100) DEFAULT NULL::character varying,
    screen_resolution character varying(100) DEFAULT NULL::character varying,
    colours character varying(100) DEFAULT NULL::character varying,
    region character varying(100) DEFAULT NULL::character varying,
    mobile boolean DEFAULT false,
    access_time timestamp without time zone DEFAULT now() NOT NULL,
    locale character varying(100) DEFAULT 'en_us'::character varying,
    path character varying(10000) DEFAULT NULL::character varying
);


ALTER TABLE nrmm.user_log OWNER TO nrmm;

--
-- TOC entry 337 (class 1259 OID 45926)
-- Name: user_log_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.user_log_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.user_log_id_seq OWNER TO nrmm;

--
-- TOC entry 6114 (class 0 OID 0)
-- Dependencies: 337
-- Name: user_log_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.user_log_id_seq OWNED BY nrmm.user_log.id;


--
-- TOC entry 338 (class 1259 OID 45928)
-- Name: user_role; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.user_role (
    id integer NOT NULL,
    users_id integer NOT NULL,
    role_id integer NOT NULL
);


ALTER TABLE nrmm.user_role OWNER TO nrmm;

--
-- TOC entry 339 (class 1259 OID 45931)
-- Name: user_role_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.user_role_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.user_role_id_seq OWNER TO nrmm;

--
-- TOC entry 6115 (class 0 OID 0)
-- Dependencies: 339
-- Name: user_role_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.user_role_id_seq OWNED BY nrmm.user_role.id;


--
-- TOC entry 340 (class 1259 OID 45933)
-- Name: user_status; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.user_status (
    userid character varying(100) NOT NULL,
    sessionid character varying(50) NOT NULL,
    ip_address character varying(100) DEFAULT NULL::character varying,
    app_path text DEFAULT NULL::character varying,
    user_agent text DEFAULT NULL::character varying,
    browser_locale character varying(100) DEFAULT NULL::character varying,
    browser character varying(100) DEFAULT NULL::character varying,
    browser_version character varying(100) DEFAULT NULL::character varying,
    os character varying(100) DEFAULT NULL::character varying,
    os_architecture character varying(100) DEFAULT NULL::character varying,
    screen_resolution character varying(100) DEFAULT NULL::character varying,
    colours character varying(100) DEFAULT NULL::character varying,
    region character varying(100) DEFAULT NULL::character varying,
    mobile boolean DEFAULT false,
    login_time timestamp without time zone DEFAULT now() NOT NULL,
    last_access timestamp without time zone,
    last_heartbeat timestamp without time zone,
    locale character varying(100) DEFAULT 'en_us'::character varying,
    info_message text DEFAULT NULL::character varying
);


ALTER TABLE nrmm.user_status OWNER TO nrmm;

--
-- TOC entry 341 (class 1259 OID 45954)
-- Name: users; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.users (
    id integer NOT NULL,
    firstname text NOT NULL,
    password text DEFAULT NULL::character varying,
    previous_passwords text DEFAULT NULL::character varying,
    address text,
    expires timestamp without time zone,
    email text DEFAULT NULL::character varying,
    phone_number text DEFAULT NULL::character varying,
    preferences text,
    disabled boolean DEFAULT false,
    send_emails boolean DEFAULT true,
    receive_emails boolean DEFAULT true,
    role_id integer,
    valid_from timestamp without time zone,
    login_fail_count integer DEFAULT 0,
    confirmed boolean DEFAULT false NOT NULL,
    new_email text DEFAULT NULL::character varying,
    lastname text,
    last_logged_in timestamp without time zone,
    borough_id integer,
    mig_userid integer[]
);


ALTER TABLE nrmm.users OWNER TO nrmm;

--
-- TOC entry 6116 (class 0 OID 0)
-- Dependencies: 341
-- Name: COLUMN users.valid_from; Type: COMMENT; Schema: nrmm; Owner: nrmm
--

COMMENT ON COLUMN nrmm.users.valid_from IS 'Date the login is valid from
Used to stop users logging in too soon';


--
-- TOC entry 6117 (class 0 OID 0)
-- Dependencies: 341
-- Name: COLUMN users.login_fail_count; Type: COMMENT; Schema: nrmm; Owner: nrmm
--

COMMENT ON COLUMN nrmm.users.login_fail_count IS 'Number of times the user has failed to login';


--
-- TOC entry 342 (class 1259 OID 45970)
-- Name: users_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.users_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.users_id_seq OWNER TO nrmm;

--
-- TOC entry 6118 (class 0 OID 0)
-- Dependencies: 342
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: nrmm; Owner: nrmm
--

ALTER SEQUENCE nrmm.users_id_seq OWNED BY nrmm.users.id;


--
-- TOC entry 343 (class 1259 OID 45972)
-- Name: workflow_id_seq; Type: SEQUENCE; Schema: nrmm; Owner: nrmm
--

CREATE SEQUENCE nrmm.workflow_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 0
    NO MAXVALUE
    CACHE 1;


ALTER TABLE nrmm.workflow_id_seq OWNER TO nrmm;

--
-- TOC entry 344 (class 1259 OID 45974)
-- Name: workflow; Type: TABLE; Schema: nrmm; Owner: nrmm
--

CREATE TABLE nrmm.workflow (
    id integer DEFAULT nextval('nrmm.workflow_id_seq'::regclass) NOT NULL,
    name character varying(200) NOT NULL,
    code character varying(200) NOT NULL,
    description character varying(500) DEFAULT NULL::character varying,
    disabled boolean DEFAULT false,
    script text NOT NULL
);


ALTER TABLE nrmm.workflow OWNER TO nrmm;

--
-- TOC entry 5649 (class 2604 OID 45983)
-- Name: auto_save id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.auto_save ALTER COLUMN id SET DEFAULT nextval('nrmm.auto_save_id_seq'::regclass);


--
-- TOC entry 5657 (class 2604 OID 45984)
-- Name: change_log id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.change_log ALTER COLUMN id SET DEFAULT nextval('nrmm.change_log_id_seq'::regclass);


--
-- TOC entry 5660 (class 2604 OID 45985)
-- Name: datasource id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.datasource ALTER COLUMN id SET DEFAULT nextval('nrmm.datasource_id_seq'::regclass);


--
-- TOC entry 5685 (class 2604 OID 45986)
-- Name: distribution_list id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.distribution_list ALTER COLUMN id SET DEFAULT nextval('nrmm.distribution_list_id_seq'::regclass);


--
-- TOC entry 5686 (class 2604 OID 45987)
-- Name: folder id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.folder ALTER COLUMN id SET DEFAULT nextval('nrmm.folder_id_seq'::regclass);


--
-- TOC entry 5689 (class 2604 OID 45988)
-- Name: log id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.log ALTER COLUMN id SET DEFAULT nextval('nrmm.log_id_seq'::regclass);


--
-- TOC entry 5699 (class 2604 OID 45989)
-- Name: media id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.media ALTER COLUMN id SET DEFAULT nextval('nrmm.media_id_seq'::regclass);


--
-- TOC entry 5653 (class 2604 OID 45990)
-- Name: media_type id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.media_type ALTER COLUMN id SET DEFAULT nextval('nrmm.case_media_type_id_seq'::regclass);


--
-- TOC entry 5704 (class 2604 OID 45991)
-- Name: note id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note ALTER COLUMN id SET DEFAULT nextval('nrmm.note_id_seq'::regclass);


--
-- TOC entry 5709 (class 2604 OID 45992)
-- Name: note_media id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note_media ALTER COLUMN id SET DEFAULT nextval('nrmm.note_media_id_seq'::regclass);


--
-- TOC entry 5710 (class 2604 OID 45993)
-- Name: note_type id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note_type ALTER COLUMN id SET DEFAULT nextval('nrmm.note_type_id_seq'::regclass);


--
-- TOC entry 5715 (class 2604 OID 45994)
-- Name: report id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report ALTER COLUMN id SET DEFAULT nextval('nrmm.report_id_seq'::regclass);


--
-- TOC entry 5720 (class 2604 OID 45995)
-- Name: report_text id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report_text ALTER COLUMN id SET DEFAULT nextval('nrmm.report_text_id_seq'::regclass);


--
-- TOC entry 5724 (class 2604 OID 45996)
-- Name: report_text_type id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report_text_type ALTER COLUMN id SET DEFAULT nextval('nrmm.report_text_type_id_seq'::regclass);


--
-- TOC entry 5727 (class 2604 OID 45997)
-- Name: role id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.role ALTER COLUMN id SET DEFAULT nextval('nrmm.role_id_seq'::regclass);


--
-- TOC entry 5732 (class 2604 OID 45998)
-- Name: role_type id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.role_type ALTER COLUMN id SET DEFAULT nextval('nrmm.role_type_id_seq'::regclass);


--
-- TOC entry 5753 (class 2604 OID 45999)
-- Name: scheduled_task id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task ALTER COLUMN id SET DEFAULT nextval('nrmm.scheduled_task_id_seq'::regclass);


--
-- TOC entry 5770 (class 2604 OID 46000)
-- Name: user_log id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_log ALTER COLUMN id SET DEFAULT nextval('nrmm.user_log_id_seq'::regclass);


--
-- TOC entry 5771 (class 2604 OID 46001)
-- Name: user_role id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_role ALTER COLUMN id SET DEFAULT nextval('nrmm.user_role_id_seq'::regclass);


--
-- TOC entry 5797 (class 2604 OID 46002)
-- Name: users id; Type: DEFAULT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.users ALTER COLUMN id SET DEFAULT nextval('nrmm.users_id_seq'::regclass);


--
-- TOC entry 5806 (class 2606 OID 46004)
-- Name: action action_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.action
    ADD CONSTRAINT action_pkey PRIMARY KEY (id);


--
-- TOC entry 5808 (class 2606 OID 46006)
-- Name: action_type action_type_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.action_type
    ADD CONSTRAINT action_type_pkey PRIMARY KEY (id);


--
-- TOC entry 5810 (class 2606 OID 46008)
-- Name: auto_save auto_save_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.auto_save
    ADD CONSTRAINT auto_save_pkey PRIMARY KEY (id);


--
-- TOC entry 5814 (class 2606 OID 46010)
-- Name: borough borough_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.borough
    ADD CONSTRAINT borough_pkey PRIMARY KEY (id);


--
-- TOC entry 5817 (class 2606 OID 46012)
-- Name: media_type case_media_type_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.media_type
    ADD CONSTRAINT case_media_type_pkey PRIMARY KEY (id);


--
-- TOC entry 5820 (class 2606 OID 46014)
-- Name: change_log change_log_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.change_log
    ADD CONSTRAINT change_log_pkey PRIMARY KEY (id);


--
-- TOC entry 5825 (class 2606 OID 46016)
-- Name: datasource datasource_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.datasource
    ADD CONSTRAINT datasource_pkey PRIMARY KEY (id);


--
-- TOC entry 5829 (class 2606 OID 46018)
-- Name: distribution_list distribution_list_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.distribution_list
    ADD CONSTRAINT distribution_list_pkey PRIMARY KEY (id);


--
-- TOC entry 5931 (class 2606 OID 46218)
-- Name: email_queue email_queue_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.email_queue
    ADD CONSTRAINT email_queue_pkey PRIMARY KEY (id);


--
-- TOC entry 5834 (class 2606 OID 46020)
-- Name: folder folder_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.folder
    ADD CONSTRAINT folder_pkey PRIMARY KEY (id);


--
-- TOC entry 5842 (class 2606 OID 46022)
-- Name: log log_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.log
    ADD CONSTRAINT log_pkey PRIMARY KEY (id);


--
-- TOC entry 5845 (class 2606 OID 46024)
-- Name: lookups lookups_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.lookups
    ADD CONSTRAINT lookups_pkey PRIMARY KEY (id);


--
-- TOC entry 5848 (class 2606 OID 46026)
-- Name: machinery machinery_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.machinery
    ADD CONSTRAINT machinery_pkey PRIMARY KEY (id);


--
-- TOC entry 5853 (class 2606 OID 46028)
-- Name: machinery_media machinerymedia_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.machinery_media
    ADD CONSTRAINT machinerymedia_pkey PRIMARY KEY (id);


--
-- TOC entry 5861 (class 2606 OID 46030)
-- Name: media media_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.media
    ADD CONSTRAINT media_pkey PRIMARY KEY (id);


--
-- TOC entry 5869 (class 2606 OID 46032)
-- Name: note_media note_media_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note_media
    ADD CONSTRAINT note_media_pkey PRIMARY KEY (id);


--
-- TOC entry 5865 (class 2606 OID 46034)
-- Name: note note_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note
    ADD CONSTRAINT note_pkey PRIMARY KEY (id);


--
-- TOC entry 5871 (class 2606 OID 46036)
-- Name: note_type note_type_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note_type
    ADD CONSTRAINT note_type_pkey PRIMARY KEY (id);


--
-- TOC entry 5873 (class 2606 OID 46038)
-- Name: patch patch_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.patch
    ADD CONSTRAINT patch_pkey PRIMARY KEY (name);


--
-- TOC entry 5875 (class 2606 OID 46040)
-- Name: report report_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report
    ADD CONSTRAINT report_pkey PRIMARY KEY (id);


--
-- TOC entry 5879 (class 2606 OID 46042)
-- Name: report_text report_text_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report_text
    ADD CONSTRAINT report_text_pkey PRIMARY KEY (id);


--
-- TOC entry 5882 (class 2606 OID 46044)
-- Name: report_text_type report_text_type_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report_text_type
    ADD CONSTRAINT report_text_type_pkey PRIMARY KEY (id);


--
-- TOC entry 5886 (class 2606 OID 46046)
-- Name: role role_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.role
    ADD CONSTRAINT role_pkey PRIMARY KEY (id);


--
-- TOC entry 5890 (class 2606 OID 46048)
-- Name: role_type role_type_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.role_type
    ADD CONSTRAINT role_type_pkey PRIMARY KEY (id);


--
-- TOC entry 5901 (class 2606 OID 46050)
-- Name: scheduled_task scheduled_task_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT scheduled_task_pkey PRIMARY KEY (id);


--
-- TOC entry 5904 (class 2606 OID 46052)
-- Name: settings settings_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.settings
    ADD CONSTRAINT settings_pkey PRIMARY KEY (name);


--
-- TOC entry 5909 (class 2606 OID 46054)
-- Name: site site_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.site
    ADD CONSTRAINT site_pkey PRIMARY KEY (id);


--
-- TOC entry 5913 (class 2606 OID 46056)
-- Name: site_users site_users_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.site_users
    ADD CONSTRAINT site_users_pkey PRIMARY KEY (site_id, users_id);


--
-- TOC entry 5936 (class 2606 OID 83789820)
-- Name: stats stats_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.stats
    ADD CONSTRAINT stats_pkey PRIMARY KEY (id);


--
-- TOC entry 5915 (class 2606 OID 46058)
-- Name: user_log user_log_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_log
    ADD CONSTRAINT user_log_pkey PRIMARY KEY (id);


--
-- TOC entry 5919 (class 2606 OID 46060)
-- Name: user_role user_role_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_role
    ADD CONSTRAINT user_role_pkey PRIMARY KEY (id);


--
-- TOC entry 5921 (class 2606 OID 46062)
-- Name: user_status user_status_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_status
    ADD CONSTRAINT user_status_pkey PRIMARY KEY (sessionid);


--
-- TOC entry 5926 (class 2606 OID 46064)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 5929 (class 2606 OID 46066)
-- Name: workflow workflow_pkey; Type: CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.workflow
    ADD CONSTRAINT workflow_pkey PRIMARY KEY (id);


--
-- TOC entry 5821 (class 1259 OID 46067)
-- Name: change_log_row_affected; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX change_log_row_affected ON nrmm.change_log USING btree (row_affected);


--
-- TOC entry 5822 (class 1259 OID 46068)
-- Name: change_log_time_added; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX change_log_time_added ON nrmm.change_log USING btree (time_added);


--
-- TOC entry 5826 (class 1259 OID 46069)
-- Name: fk_cache_trigger_report_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_cache_trigger_report_id ON nrmm.datasource USING btree (cache_trigger_report_id);


--
-- TOC entry 5830 (class 1259 OID 46070)
-- Name: fk_distribution_list_datasource; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_distribution_list_datasource ON nrmm.distribution_list USING btree (datasource_id);


--
-- TOC entry 5832 (class 1259 OID 46071)
-- Name: fk_folder_parent_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_folder_parent_id ON nrmm.folder USING btree (parent);


--
-- TOC entry 5836 (class 1259 OID 46072)
-- Name: fk_log_task_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_log_task_id ON nrmm.log USING btree (task_id);


--
-- TOC entry 5851 (class 1259 OID 46073)
-- Name: fk_machinery_media_media_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_machinery_media_media_id ON nrmm.machinery_media USING btree (media_id);


--
-- TOC entry 5866 (class 1259 OID 46074)
-- Name: fk_note_media_media_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_note_media_media_id ON nrmm.note_media USING btree (media_id);


--
-- TOC entry 5867 (class 1259 OID 46075)
-- Name: fk_note_media_note_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_note_media_note_id ON nrmm.note_media USING btree (note_id);


--
-- TOC entry 5876 (class 1259 OID 46076)
-- Name: fk_report_text_type_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_report_text_type_id ON nrmm.report_text USING btree (type_id);


--
-- TOC entry 5883 (class 1259 OID 46077)
-- Name: fk_role_role_type_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_role_role_type_id ON nrmm.role USING btree (type_id);


--
-- TOC entry 5891 (class 1259 OID 46078)
-- Name: fk_task_datasource_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_datasource_id ON nrmm.scheduled_task USING btree (datasource_id);


--
-- TOC entry 5892 (class 1259 OID 46079)
-- Name: fk_task_datasource_id1; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_datasource_id1 ON nrmm.scheduled_task USING btree (datasource_id1);


--
-- TOC entry 5893 (class 1259 OID 46080)
-- Name: fk_task_datasource_id2; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_datasource_id2 ON nrmm.scheduled_task USING btree (datasource_id2);


--
-- TOC entry 5894 (class 1259 OID 46081)
-- Name: fk_task_datasource_id3; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_datasource_id3 ON nrmm.scheduled_task USING btree (datasource_id3);


--
-- TOC entry 5895 (class 1259 OID 46082)
-- Name: fk_task_datasource_id4; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_datasource_id4 ON nrmm.scheduled_task USING btree (datasource_id4);


--
-- TOC entry 5896 (class 1259 OID 46083)
-- Name: fk_task_distribution_list_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_distribution_list_id ON nrmm.scheduled_task USING btree (distribution_list_id);


--
-- TOC entry 5897 (class 1259 OID 46084)
-- Name: fk_task_error_distribution_list_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_error_distribution_list_id ON nrmm.scheduled_task USING btree (error_distribution_list_id);


--
-- TOC entry 5898 (class 1259 OID 46085)
-- Name: fk_task_notify_distribution_list_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_notify_distribution_list_id ON nrmm.scheduled_task USING btree (notify_distribution_list_id);


--
-- TOC entry 5899 (class 1259 OID 46086)
-- Name: fk_task_report_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_task_report_id ON nrmm.scheduled_task USING btree (report_id);


--
-- TOC entry 5916 (class 1259 OID 46087)
-- Name: fk_user_role_role_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_user_role_role_id ON nrmm.user_role USING btree (role_id);


--
-- TOC entry 5917 (class 1259 OID 46088)
-- Name: fk_user_role_users_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX fk_user_role_users_id ON nrmm.user_role USING btree (users_id);


--
-- TOC entry 5811 (class 1259 OID 46089)
-- Name: idx_auto_save_reference_type; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_auto_save_reference_type ON nrmm.auto_save USING btree (reference_type);


--
-- TOC entry 5812 (class 1259 OID 46090)
-- Name: idx_auto_save_users_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_auto_save_users_id ON nrmm.auto_save USING btree (users_id);


--
-- TOC entry 5815 (class 1259 OID 46091)
-- Name: idx_borough_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_borough_unique_name ON nrmm.borough USING btree (lower(name));


--
-- TOC entry 5818 (class 1259 OID 46092)
-- Name: idx_case_media_type_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_case_media_type_unique_name ON nrmm.media_type USING btree (lower((name)::text));


--
-- TOC entry 5823 (class 1259 OID 46093)
-- Name: idx_change_log_table_affected; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_change_log_table_affected ON nrmm.change_log USING btree (table_affected);


--
-- TOC entry 5827 (class 1259 OID 46094)
-- Name: idx_datasource_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_datasource_unique_name ON nrmm.datasource USING btree (lower((name)::text));


--
-- TOC entry 5831 (class 1259 OID 46095)
-- Name: idx_distribution_list_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_distribution_list_unique_name ON nrmm.distribution_list USING btree (lower((name)::text));


--
-- TOC entry 5862 (class 1259 OID 46096)
-- Name: idx_fk_note_user_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_fk_note_user_id ON nrmm.note USING btree (user_id);


--
-- TOC entry 5837 (class 1259 OID 46097)
-- Name: idx_log_date_server_report; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_log_date_server_report ON nrmm.log USING btree (date_added, server_id, report_name);


--
-- TOC entry 5838 (class 1259 OID 46098)
-- Name: idx_log_report_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_log_report_name ON nrmm.log USING btree (report_name);


--
-- TOC entry 5839 (class 1259 OID 46099)
-- Name: idx_log_status_report_date; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_log_status_report_date ON nrmm.log USING btree (status, report_name);


--
-- TOC entry 5840 (class 1259 OID 46100)
-- Name: idx_log_task_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_log_task_name ON nrmm.log USING btree (task_name);


--
-- TOC entry 5854 (class 1259 OID 46101)
-- Name: idx_media_description; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_description ON nrmm.media USING btree (description);


--
-- TOC entry 5855 (class 1259 OID 46102)
-- Name: idx_media_extension; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_extension ON nrmm.media USING btree (extension);


--
-- TOC entry 5856 (class 1259 OID 46103)
-- Name: idx_media_filename; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_filename ON nrmm.media USING btree (filename);


--
-- TOC entry 5857 (class 1259 OID 46104)
-- Name: idx_media_folder; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_folder ON nrmm.media USING btree (folder);


--
-- TOC entry 5858 (class 1259 OID 46105)
-- Name: idx_media_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_name ON nrmm.media USING btree (name);


--
-- TOC entry 5859 (class 1259 OID 46106)
-- Name: idx_media_type; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_media_type ON nrmm.media USING btree (type);


--
-- TOC entry 5863 (class 1259 OID 46107)
-- Name: idx_note_reference_id_reference_type; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_note_reference_id_reference_type ON nrmm.note USING btree (reference_id, reference_type);


--
-- TOC entry 5880 (class 1259 OID 46108)
-- Name: idx_report_text_type_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_report_text_type_unique_name ON nrmm.report_text_type USING btree (lower((name)::text));


--
-- TOC entry 5877 (class 1259 OID 46109)
-- Name: idx_report_text_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_report_text_unique_name ON nrmm.report_text USING btree (lower((name)::text));


--
-- TOC entry 5884 (class 1259 OID 46110)
-- Name: idx_role_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_role_name ON nrmm.role USING btree (lower((name)::text));


--
-- TOC entry 5887 (class 1259 OID 46111)
-- Name: idx_role_type_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_role_type_id ON nrmm.role_type USING btree (id);


--
-- TOC entry 5888 (class 1259 OID 46112)
-- Name: idx_role_type_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_role_type_name ON nrmm.role_type USING btree (name);


--
-- TOC entry 5902 (class 1259 OID 46113)
-- Name: idx_settings_unique_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_settings_unique_name ON nrmm.settings USING btree (lower((name)::text));


--
-- TOC entry 5932 (class 1259 OID 83789821)
-- Name: idx_stats_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_stats_name ON nrmm.stats USING btree (name);


--
-- TOC entry 5933 (class 1259 OID 83789822)
-- Name: idx_stats_time_added; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_stats_time_added ON nrmm.stats USING btree (time_added);


--
-- TOC entry 5934 (class 1259 OID 83789823)
-- Name: idx_stats_type; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_stats_type ON nrmm.stats USING btree (type);


--
-- TOC entry 5835 (class 1259 OID 46114)
-- Name: idx_unique_folder_within_parent; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_unique_folder_within_parent ON nrmm.folder USING btree (name, parent);


--
-- TOC entry 5843 (class 1259 OID 46115)
-- Name: idx_unique_lu_nametype; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_unique_lu_nametype ON nrmm.lookups USING btree (name, type);


--
-- TOC entry 5927 (class 1259 OID 46116)
-- Name: idx_unique_workflow_code; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_unique_workflow_code ON nrmm.workflow USING btree (code);


--
-- TOC entry 5922 (class 1259 OID 46117)
-- Name: idx_users_email_unique; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE UNIQUE INDEX idx_users_email_unique ON nrmm.users USING btree (email);


--
-- TOC entry 5923 (class 1259 OID 46118)
-- Name: idx_users_name; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_users_name ON nrmm.users USING btree (firstname);


--
-- TOC entry 5924 (class 1259 OID 46119)
-- Name: idx_users_receive_emails; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX idx_users_receive_emails ON nrmm.users USING btree (receive_emails);


--
-- TOC entry 5846 (class 1259 OID 46220)
-- Name: machinery_end_date_idx; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX machinery_end_date_idx ON nrmm.machinery USING btree (end_date);


--
-- TOC entry 5849 (class 1259 OID 46221)
-- Name: machinery_site_id; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX machinery_site_id ON nrmm.machinery USING btree (site_id);


--
-- TOC entry 5850 (class 1259 OID 46222)
-- Name: machinery_start_date_idx; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX machinery_start_date_idx ON nrmm.machinery USING btree (start_date);


--
-- TOC entry 5905 (class 1259 OID 46223)
-- Name: site_borough_idx; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX site_borough_idx ON nrmm.site USING btree (borough_id);


--
-- TOC entry 5906 (class 1259 OID 46224)
-- Name: site_end_date_idx; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX site_end_date_idx ON nrmm.site USING btree (end_date);


--
-- TOC entry 5907 (class 1259 OID 46225)
-- Name: site_name_idx; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX site_name_idx ON nrmm.site USING btree (name);


--
-- TOC entry 5910 (class 1259 OID 46226)
-- Name: site_postcode_idx; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX site_postcode_idx ON nrmm.site USING btree (postcode);


--
-- TOC entry 5911 (class 1259 OID 46227)
-- Name: site_start_date_idx; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX site_start_date_idx ON nrmm.site USING btree (start_date);


--
-- TOC entry 5937 (class 1259 OID 83789824)
-- Name: stats_time_added; Type: INDEX; Schema: nrmm; Owner: nrmm
--

CREATE INDEX stats_time_added ON nrmm.stats USING btree (time_added);


--
-- TOC entry 5956 (class 2620 OID 88716568)
-- Name: machinery admin_user_id; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER admin_user_id BEFORE INSERT OR DELETE OR UPDATE ON nrmm.machinery FOR EACH ROW EXECUTE PROCEDURE nrmm.machinery_admin_user_id();


--
-- TOC entry 6119 (class 0 OID 0)
-- Dependencies: 5956
-- Name: TRIGGER admin_user_id ON machinery; Type: COMMENT; Schema: nrmm; Owner: nrmm
--

COMMENT ON TRIGGER admin_user_id ON nrmm.machinery IS 'Keeps admin_user_id and use_id the same';


--
-- TOC entry 5955 (class 2620 OID 46120)
-- Name: change_log change_log_time_added_insert_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER change_log_time_added_insert_timestamp BEFORE INSERT ON nrmm.change_log FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_added();


--
-- TOC entry 5963 (class 2620 OID 46219)
-- Name: email_queue email_queue_time_added_insert_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER email_queue_time_added_insert_timestamp BEFORE INSERT ON nrmm.email_queue FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_added();


--
-- TOC entry 5954 (class 2620 OID 88731790)
-- Name: action hit_count; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER hit_count BEFORE UPDATE OF used, hit_count ON nrmm.action FOR EACH ROW WHEN (((new.hit_count < 3) AND (old.used = false))) EXECUTE PROCEDURE nrmm.action_hit();


--
-- TOC entry 6120 (class 0 OID 0)
-- Dependencies: 5954
-- Name: TRIGGER hit_count ON action; Type: COMMENT; Schema: nrmm; Owner: nrmm
--

COMMENT ON TRIGGER hit_count ON nrmm.action IS 'Process action hit';


--
-- TOC entry 5957 (class 2620 OID 46121)
-- Name: media media_time_added_insert_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER media_time_added_insert_timestamp BEFORE INSERT ON nrmm.media FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_added();


--
-- TOC entry 5958 (class 2620 OID 46122)
-- Name: media media_time_modified_update_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER media_time_modified_update_timestamp BEFORE UPDATE ON nrmm.media FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_modified();


--
-- TOC entry 5959 (class 2620 OID 46123)
-- Name: note note_time_added_insert_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER note_time_added_insert_timestamp BEFORE INSERT ON nrmm.note FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_added();


--
-- TOC entry 5960 (class 2620 OID 46124)
-- Name: patch patch_time_added_insert_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER patch_time_added_insert_timestamp BEFORE INSERT ON nrmm.patch FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_added();


--
-- TOC entry 5961 (class 2620 OID 46125)
-- Name: report report_last_modified_update_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER report_last_modified_update_timestamp BEFORE UPDATE ON nrmm.report FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_modified();


--
-- TOC entry 5962 (class 2620 OID 46126)
-- Name: user_log user_log_access_time_update_timestamp; Type: TRIGGER; Schema: nrmm; Owner: nrmm
--

CREATE TRIGGER user_log_access_time_update_timestamp BEFORE UPDATE ON nrmm.user_log FOR EACH ROW EXECUTE PROCEDURE nrmm.set_time_modified();


--
-- TOC entry 5938 (class 2606 OID 46127)
-- Name: datasource fk_datasource_report; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.datasource
    ADD CONSTRAINT fk_datasource_report FOREIGN KEY (cache_trigger_report_id) REFERENCES nrmm.report(id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 5939 (class 2606 OID 46132)
-- Name: distribution_list fk_distribution_list_datasource; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.distribution_list
    ADD CONSTRAINT fk_distribution_list_datasource FOREIGN KEY (datasource_id) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 5940 (class 2606 OID 46137)
-- Name: note fk_note_user_id; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.note
    ADD CONSTRAINT fk_note_user_id FOREIGN KEY (user_id) REFERENCES nrmm.users(id) DEFERRABLE;


--
-- TOC entry 5941 (class 2606 OID 46142)
-- Name: report_text fk_report_text_type; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.report_text
    ADD CONSTRAINT fk_report_text_type FOREIGN KEY (type_id) REFERENCES nrmm.report_text_type(id) DEFERRABLE;


--
-- TOC entry 5942 (class 2606 OID 46147)
-- Name: role fk_role_role_type; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.role
    ADD CONSTRAINT fk_role_role_type FOREIGN KEY (type_id) REFERENCES nrmm.role_type(id) DEFERRABLE;


--
-- TOC entry 5943 (class 2606 OID 46152)
-- Name: scheduled_task fk_scheduled_task; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task FOREIGN KEY (distribution_list_id) REFERENCES nrmm.distribution_list(id) DEFERRABLE;


--
-- TOC entry 5944 (class 2606 OID 46157)
-- Name: scheduled_task fk_scheduled_task_datasource; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_datasource FOREIGN KEY (datasource_id) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 5945 (class 2606 OID 46162)
-- Name: scheduled_task fk_scheduled_task_datasource1; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_datasource1 FOREIGN KEY (datasource_id1) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 5946 (class 2606 OID 46167)
-- Name: scheduled_task fk_scheduled_task_datasource2; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_datasource2 FOREIGN KEY (datasource_id2) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 5947 (class 2606 OID 46172)
-- Name: scheduled_task fk_scheduled_task_datasource3; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_datasource3 FOREIGN KEY (datasource_id3) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 5948 (class 2606 OID 46177)
-- Name: scheduled_task fk_scheduled_task_datasource4; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_datasource4 FOREIGN KEY (datasource_id4) REFERENCES nrmm.datasource(id) DEFERRABLE;


--
-- TOC entry 5949 (class 2606 OID 46182)
-- Name: scheduled_task fk_scheduled_task_error_distribution_list; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_error_distribution_list FOREIGN KEY (error_distribution_list_id) REFERENCES nrmm.distribution_list(id) DEFERRABLE;


--
-- TOC entry 5950 (class 2606 OID 46187)
-- Name: scheduled_task fk_scheduled_task_notify_distribution_list; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_notify_distribution_list FOREIGN KEY (notify_distribution_list_id) REFERENCES nrmm.distribution_list(id) DEFERRABLE;


--
-- TOC entry 5951 (class 2606 OID 46192)
-- Name: scheduled_task fk_scheduled_task_report; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.scheduled_task
    ADD CONSTRAINT fk_scheduled_task_report FOREIGN KEY (report_id) REFERENCES nrmm.report(id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 5952 (class 2606 OID 46197)
-- Name: user_role fk_user_role_role; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_role
    ADD CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES nrmm.role(id) DEFERRABLE;


--
-- TOC entry 5953 (class 2606 OID 46202)
-- Name: user_role fk_user_role_users; Type: FK CONSTRAINT; Schema: nrmm; Owner: nrmm
--

ALTER TABLE ONLY nrmm.user_role
    ADD CONSTRAINT fk_user_role_users FOREIGN KEY (users_id) REFERENCES nrmm.users(id) DEFERRABLE;


-- Completed on 2020-08-20 10:00:08 BST

--
-- PostgreSQL database dump complete
--

